package MIP;

import Common.Data;
import Common.Pod;
import Common.Segment;
import Common.Solution;
import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.cplex.IloCplex;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.*;

public class MIP {
    public Data data;
    public IloCplex model;
    public Solution solution;

    public IloNumVar[][][] x;
    public IloNumVar[][][] y; //
    public IloNumVar[][] omega; //
    public IloNumVar[][] delta; //
    public IloNumVar[][] a; //
    public IloNumVar[] alpha; //

    public boolean[][] A;

    int N;
    public double M = 10000;
    public double TimeLimit = 1800;
    String fileName;
    PrintStream ps;
    PrintStream out;

    public MIP(Data data, String _fileName) throws FileNotFoundException {
        this.data = data;
        N = data.N;
        fileName = _fileName;
        ps = new PrintStream(fileName + ".txt");
        out = System.out;
        System.setOut(ps);
    }

    public void buildGraph() {
        A = new boolean[data.N][data.N];
        for (int i = 0; i < data.N; i++) {
            Arrays.fill(A[i], true);
            A[i][i] = false;
        }
    }

    public Solution solve() throws IloException, FileNotFoundException {
        if (model.solve() == false) {
            System.setOut(out);
            // 模型不可解
            System.out.println("problem should not solve false!!!");
            return null;
        } else {
            System.setOut(out);
            System.out.println("problem could solve.");
            System.out.println("obj = " + model.getObjValue());
//            showDetails();
            solution = createSolution();
        }
        return solution;
    }

    public void buildModel() throws IloException {
        // model
        model = new IloCplex();
        model.setParam(IloCplex.DoubleParam.TiLim, TimeLimit);

        buildGraph();

        // variables
        x = new IloNumVar[data.Nv][N][N];
        y = new IloNumVar[data.L + 1][N][N];
        omega = new IloNumVar[data.Nv][N];
        delta = new IloNumVar[data.Nv][N];
        a = new IloNumVar[data.Nv][N];
        alpha = new IloNumVar[N];

        // 定义域
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                if (A[i][j] || (i == 0 && j == 0)) {
                    for (int k = 0; k < data.Nv; k++) {
                        x[k][i][j] = model.numVar(0, 1, IloNumVarType.Bool, "x_" + k + "," + i + "," + j);
                    }
                    for (int l = 1; l <= data.L; l++) {
                        y[l][i][j] = model.numVar(0, 1, IloNumVarType.Bool, "y_" + l + "," + i + "," + j);
                    }
                }
            }
        }

        for (int i = 0; i < N; i++) {
            for (int k = 0; k < data.Nv; k++) {
                omega[k][i] = model.numVar(0, 1, IloNumVarType.Bool, "o_" + k + "," + i);
                delta[k][i] = model.numVar(0, data.capacity, IloNumVarType.Float, "d_" + k + "," + i);
                a[k][i] = model.numVar(data.ETW[i], data.LTW[i], IloNumVarType.Float, "a_" + k + "," + i);

            }
            alpha[i] = model.numVar(data.ETW[i], data.LTW[i], IloNumVarType.Float, "alpha_" + i);
        }

        // objective function (1)
        IloNumExpr obj = model.numExpr();
        for (int i = 0; i < data.N; i++) {
            for (int j = 0; j < data.N; j++) {
                if (A[i][j]) {
                    for (int l = 1; l <= data.L; l++) {
                        obj = model.sum(obj, model.prod(data.cost_para[l] * data.dis[i][j], y[l][i][j]));
                    }
                }
            }
        }
        model.addMinimize(obj);

        // Constraints (2)
        for (int j = 1; j < data.N; j++) {
            IloNumExpr expr1 = model.numExpr();
            for (int i = 0; i < data.N; i++) {
                if (A[i][j]) {
                    for (int k = 0; k < data.Nv; k++) {
                        expr1 = model.sum(expr1, x[k][i][j]);
                    }
                }
            }
            model.addGe(expr1, 1);
        }

        // Constraints (3)
        for (int k = 0; k < data.Nv; k++) {
            for (int j = 1; j < data.N; j++) {
                IloNumExpr expr2 = model.numExpr();
                IloNumExpr expr3 = model.numExpr();
                for (int i = 0; i < data.N; i++) {
                    if (A[i][j]) {
                        expr2 = model.sum(expr2, x[k][i][j]);
                    }
                    if (A[j][i]) {
                        expr3 = model.sum(expr3, x[k][j][i]);
                    }
                }
                model.addEq(expr2, expr3);
            }
        }

        // Constraints (4)
        for (int k = 0; k < data.Nv; k++) {
            IloNumExpr expr2 = model.numExpr();
            IloNumExpr expr3 = model.numExpr();
            for (int j = 0; j < data.N; j++) {
                expr2 = model.sum(expr2, x[k][0][j]);
                expr3 = model.sum(expr3, x[k][j][0]);
            }
            model.addEq(expr2, 1);
            model.addEq(expr3, 1);
        }

        // Constraints (5)
        for (int i = 0; i < data.N; i++) {
            for (int j = 1; j < data.N; j++) {
                if (A[i][j]) {
                    for (int k = 0; k < data.Nv; k++) {
                        IloNumExpr expr4 = model.numExpr();
                        expr4 = model.sum(expr4, alpha[i]);
                        expr4 = model.sum(expr4, data.dis[i][j] + data.service_time[i]);
                        IloNumExpr subExpr = model.numExpr();
                        subExpr = model.sum(subExpr, 1);
                        subExpr = model.diff(subExpr, x[k][i][j]);
                        double M_i = data.N;
                        expr4 = model.diff(expr4, model.prod(M, subExpr));
                        IloNumExpr expr5 = model.numExpr();
                        expr5 = model.sum(expr5, a[k][j]);
                        model.addLe(expr4, expr5);
                    }
                }
            }
        }

        for (int i = 0; i < data.N; i++) {
            for (int k = 0; k < data.Nv; k++) {
                IloNumExpr expr4 = model.numExpr();
                expr4 = model.sum(expr4, a[k][i]);
                IloNumExpr expr5 = model.numExpr();
                expr5 = model.sum(expr5, alpha[i]);
                model.addLe(expr4, expr5);
            }
        }

        // Constraints (6)
        for (int i = 0; i < data.N; i++) {
            for (int j = 1; j < data.N; j++) {
                if (A[i][j]) {
                    for (int k = 0; k < data.Nv; k++) {
                        IloNumExpr expr4 = model.numExpr();
                        expr4 = model.sum(expr4, delta[k][i]);
                        expr4 = model.sum(expr4, data.demands[j]);
                        IloNumExpr subExpr = model.numExpr();
                        subExpr = model.sum(subExpr, 2);
                        subExpr = model.diff(subExpr, x[k][i][j]);
                        subExpr = model.diff(subExpr, omega[k][j]);
                        double M_i = data.capacity + data.demands[i];
                        expr4 = model.diff(expr4, model.prod(M_i, subExpr));
                        IloNumExpr expr5 = model.numExpr();
                        expr5 = model.sum(expr5, delta[k][j]);
                        model.addLe(expr4, expr5);
                    }
                }
            }
        }

        // Constraints (7)
        for (int i = 0; i < data.N; i++) {
            for (int j = 1; j < data.N; j++) {
                if (A[i][j]) {
                    for (int k = 0; k < data.Nv; k++) {
                        IloNumExpr expr4 = model.numExpr();
                        expr4 = model.sum(expr4, delta[k][i]);
                        IloNumExpr subExpr = model.numExpr();
                        subExpr = model.sum(subExpr, 1);
                        subExpr = model.diff(subExpr, x[k][i][j]);
                        double M_i = data.capacity;
                        expr4 = model.diff(expr4, model.prod(M_i, subExpr));
                        IloNumExpr expr5 = model.numExpr();
                        expr5 = model.sum(expr5, delta[k][j]);
                        model.addLe(expr4, expr5);
                    }
                }
            }
        }

        // Constraints (8)
        for (int i = 1; i < data.N; i++) {
            IloNumExpr expr6 = model.numExpr();
            for (int j = 0; j < data.N; j++) {
                for (int k = 0; k < data.Nv; k++) {
                    expr6 = model.sum(expr6, omega[k][i]);
                }
            }
            model.addGe(expr6, 1);
        }

        // Constraints (9)
        for (int j = 1; j < data.N; j++) {
            for (int k = 0; k < data.Nv; k++) {
                IloNumExpr expr7 = model.numExpr();
                for (int i = 0; i < data.N; i++) {
                    if (A[i][j]) {
                        expr7 = model.sum(expr7, x[k][i][j]);
                    }
                }
                expr7 = model.diff(expr7, omega[k][j]);
                model.addGe(expr7, 0);
            }
        }

        // Constraints (10)
        for (int j = 1; j < data.N; j++) {
            for (int i = 0; i < data.N; i++) {
                if (A[i][j]) {
                    IloNumExpr expr8 = model.numExpr();
                    for (int k = 0; k < data.Nv; k++) {
                        expr8 = model.sum(expr8, x[k][i][j]);
                    }
                    model.addLe(expr8, data.L);
                }
            }
        }

        // Constraints (11) - for basic model
        for (int i = 0; i < data.N; i++) {
            for (int j = 0; j < data.N; j++) {
                if (A[i][j]) {
                    IloNumExpr expr9 = model.numExpr();
                    IloNumExpr expr10 = model.numExpr();
                    for (int l = 1; l <= data.L; l++) {
                        expr9 = model.sum(expr9, y[l][i][j]);
                    }
                    for (int k = 0; k < data.Nv; k++) {
                        expr10 = model.sum(expr10, x[k][i][j]);
                    }
                    expr10 = model.prod(expr10, 1.0 / data.L);
                    model.addGe(expr9, expr10);
                }
            }
        }

        // Constraints (12)
        for (int i = 0; i < data.N; i++) {
            for (int j = 0; j < data.N; j++) {
                if (A[i][j]) {
                    for (int l = 1; l <= data.L; l++) {
                        IloNumExpr expr11 = model.numExpr();
                        IloNumExpr expr12 = model.numExpr();
                        expr11 = model.sum(expr11, y[l][i][j]);
                        expr11 = model.diff(expr11, 1);
                        expr12 = model.sum(expr12, l);
                        for (int k = 0; k < data.Nv; k++) {
                            expr12 = model.diff(expr12, x[k][i][j]);
                        }
                        expr12 = model.prod(expr12, 1.0 / data.L);
                        model.addLe(expr11, expr12);
                    }
                }
            }
        }

//        ------

//        // Constraints (16)
//        for (int i = 0; i < data.N; i++) {
//            for (int j = 0; j < data.N; j++) {
//                if (A[i][j]) {
//                    for (int l = 1; l <= data.lm; l++) {
//                        IloNumExpr expr9 = model.numExpr();
//                        expr9 = model.sum(expr9, t[l][i][j]);
//                        for (int k = 0; k < data.np; k++) {
//                            expr9 = model.diff(expr9, x[k][i][j]);
//                        }
//                        expr9 = model.sum(expr9, l);
//                        model.addGe(expr9, 0);
//                    }
//                }
//            }
//        }
//
//        // Constraints (17)
//        for (int i = 0; i < data.N; i++) {
//            for (int j = 0; j < data.N; j++) {
//                if (A[i][j]) {
//                    for (int l = 1; l <= data.lm; l++) {
//                        IloNumExpr expr10 = model.numExpr();
//                        expr10 = model.sum(expr10, t_[l][i][j]);
//                        for (int k = 0; k < data.np; k++) {
//                            expr10 = model.sum(expr10, x[k][i][j]);
//                        }
//                        expr10 = model.diff(expr10, l);
//                        model.addGe(expr10, 0);
//                    }
//                }
//            }
//        }
//
//        // Constraints (18)
//        for (int i = 0; i < data.N; i++) {
//            for (int j = 0; j < data.N; j++) {
//                if (A[i][j]) {
//                    for (int l = 1; l <= data.lm; l++) {
//                        IloNumExpr expr10 = model.numExpr();
//                        expr10 = model.sum(expr10, t[l][i][j]);
//                        expr10 = model.diff(expr10, t_[l][i][j]);
//                        for (int k = 0; k < data.np; k++) {
//                            expr10 = model.diff(expr10, x[k][i][j]);
//                        }
//                        expr10 = model.sum(expr10, l);
//                        model.addEq(expr10, 0);
//                    }
//                }
//            }
//        }
//
//        // Constraints (19)
//        for (int i = 0; i < data.N; i++) {
//            for (int j = 0; j < data.N; j++) {
//                if (A[i][j]) {
//                    for (int l = 1; l <= data.lm; l++) {
//                        IloNumExpr expr11 = model.numExpr();
//                        expr11 = model.sum(expr11, y[l][i][j]);
//                        expr11 = model.sum(expr11, t[l][i][j]);
//                        expr11 = model.sum(expr11, t_[l][i][j]);
//                        model.addGe(expr11, 1);
//                    }
//                }
//            }
//        }


        // print information
//        model.exportModel("model.lp");
    }

    private void showDetails() throws IloException {
        StringBuilder str = new StringBuilder();
        for (int k = 0; k < data.Nv; k++) {
            for (int i = 0; i < data.N; i++) {
                for (int j = 0; j < data.N; j++) {
                    if (A[i][j]) {
                        if (model.getValue(x[k][i][j]) > 0.9)
                            str.append("x_{" + k + i + j + "}=" + model.getValue(x[k][i][j]) + ";");
                    }
                }
                str.append("\n");
            }
        }
        for (int l = 1; l <= data.L; l++) {
            for (int i = 0; i < data.N; i++) {
                for (int j = 0; j < data.N; j++) {
                    if (A[i][j]) {
                        if (model.getValue(y[l][i][j]) > 0.9)
                            str.append("y_{" + l + i + j + "}=" + model.getValue(y[l][i][j]) + ";");
                    }
                }
                str.append("\n");
            }
        }
        for (int k = 0; k < data.Nv; k++) {
            for (int i = 1; i < N; i++) {
                if (model.getValue(omega[k][i]) > 0.9)
                    str.append("o_{" + k + i + "}=" + model.getValue(omega[k][i]) + ";");
            }
            str.append("\n");
        }
        for (int k = 0; k < data.Nv; k++) {
            for (int i = 0; i < N; i++) {
                if (model.getValue(delta[k][i]) > 0.9)
                    str.append("d_{" + k + i + "}=" + model.getValue(delta[k][i]) + ";");
            }
            str.append("\n");
        }
//        for (int i = 0; i < N; i++) {
//            if (model.getValue(a[i]) > 0.9)
//                str.append("u_{" + i + "}=" + model.getValue(a[i]) + ";");
//        }
//        str.append("\n");

//        for (int l = 1; l <= data.lm; l++) {
//            for (int i = 0; i < data.N; i++) {
//                for (int j = 0; j < data.N; j++) {
//                    if (A[i][j]) {
//                        if (model.getValue(t[l][i][j]) > 0.9)
//                            str.append("t1_{" + l + i + j + "}=" + model.getValue(t[l][i][j]) + ";");
//                    }
//                }
//                str.append("\n");
//            }
//        }

//        str.append("\n");
//        for (int l = 1; l <= data.lm; l++) {
//            for (int i = 0; i < data.N; i++) {
//                for (int j = 0; j < data.N; j++) {
//                    if (A[i][j]) {
//                        if (model.getValue(t_[l][i][j]) > 0.9)
//                            str.append("t2_{" + l + i + j + "}=" + model.getValue(t_[l][i][j]) + ";");
//                    }
//                }
//                str.append("\n");
//            }
//        }
//        str.append("\n");

        str.append("\n");

        System.out.println(str);
    }

    private Solution createSolution() throws IloException {
        Solution sol = new Solution(data);

        ArrayList<Integer> pod_num = new ArrayList<>();
        HashSet<Integer> first_node_list = new HashSet<>();
        PriorityQueue<Integer> begin_list = new PriorityQueue<>();
        begin_list.add(0);
        while (!begin_list.isEmpty()) {
            int begin = (int) (begin_list.poll());
            first_node_list.add(begin);
            int l = 1;
            HashSet<Integer> second_node_list = new HashSet<>();
            while (l <= data.L) {
                ArrayList<Integer> s_route = new ArrayList<>();
                int before = begin;
                s_route.add(before);
                continueOut:
                while (true) {
                    for (int i = 0; i < data.N; i++) {
                        if (A[before][i] && model.getValue(y[l][before][i]) >= 0.9) {
                            if (before == begin) {
                                if (second_node_list.contains(i)) {
                                    continue;
                                } else {
                                    second_node_list.add(i);
                                }
                            }
                            s_route.add(i);
                            before = i;
                            if (i == 0)
                                break;
                            continue continueOut;
                        }
                    }
                    break;
                }
                if (s_route.size() > 1) {
                    sol.segments.add(new Segment(s_route, 0, true, data));
                    pod_num.add(l);
                    if (!first_node_list.contains(before) && !begin_list.contains(before) && before != 0)
                        begin_list.add(before); // 一个segment结束
                } else {
                    l++;
                    second_node_list = new HashSet<>();
                }
            }
        }

        sol.buildDAG();

        for (int k = 0; k < data.Nv; k++) {
            sol.pods.add(new Pod(data));
        }
        for (Segment seg : sol.segments) {
            for (int k = 0; k < data.Nv; k++) {
                if (model.getValue(x[k][seg.route.get(0)][seg.route.get(1)]) >= 0.9) {
                    sol.pods.get(k).segments.add(seg);
                    seg.related_pods.add(sol.pods.get(k));
                    HashSet<Integer> pods_cus = new HashSet<>();
                    for (int i = 0; i < seg.route.size() - 1; i++) {
                        if (seg.route.get(i) != 0 && model.getValue(omega[k][seg.route.get(i)]) >= 0.9) {
                            pods_cus.add(seg.route.get(i));
                        }
                    }
                    sol.pods.get(k).customers.put(seg, pods_cus);
                }
            }
        }

        sol.updateInfo(false);

        for (int i = 0; i < sol.pods.size(); i++) {
            if (sol.pods.get(i).demand == 0) {
                sol.pods.remove(i);
                i--;
            }
        }


        return sol;
    }
}