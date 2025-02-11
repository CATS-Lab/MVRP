package TS;

import Common.Data;
import Common.Pod;
import Common.Segment;
import Common.Solution;
import Greedy.CWMove;

import java.util.ArrayList;
import java.util.Arrays;

public class MoveFinder {
    public Data data;
    public int[][] tabu_list_re;
    public int[][] tabu_list_ex;
    public int tabu_list_mo;

    public MoveFinder(Data _data) {
        data = _data;
        tabu_list_re = new int[data.N][data.N];
        tabu_list_ex = new int[data.N][data.N];
        tabu_list_mo = 0;
        for (int i = 0; i < this.tabu_list_re.length; i++) {
            Arrays.fill(tabu_list_re[i], 0);
            Arrays.fill(tabu_list_ex[i], 0);
        }
    }

    public SegRelocateMove findBestSegReMove(Solution sol, int iteration, Solution bestSolution) {
        SegRelocateMove best_move = null;
        double best_cost = data.big_num;
        for (int i = 0; i < sol.segments.size(); i++) {
            Segment seg = sol.segments.get(i);
            for (int j = 1; j < seg.route.size() - 1; j++) {
                for (int k = 1; k < seg.route.size() - 1; k++) {
                    if (j == k || j == k - 1) continue;
                    SegRelocateMove new_move = new SegRelocateMove(data, i, j, k);
                    boolean is_feasible = new_move.evaluate(sol);
                    if (!is_feasible) continue;
                    ArrayList<TabuArc> arc_list = new ArrayList<>();
                    arc_list.add(new TabuArc(seg.route.get(j - 1), seg.route.get(j)));
                    arc_list.add(new TabuArc(seg.route.get(j), seg.route.get(j + 1)));
                    arc_list.add(new TabuArc(seg.route.get(k - 1), seg.route.get(k)));
                    boolean newMoveTabu = isTabu_re(arc_list, iteration);
                    if (new_move.add_cost < best_cost) {
                        if (new_move.add_cost + sol.cost < bestSolution.cost || (!newMoveTabu)) {
                            best_move = new_move;
                            best_cost = new_move.add_cost;
                        }
                    }
                }
            }
        }
        return best_move;
    }

    public PodRelocateMove findBestPodReMove(Solution sol, int iteration, Solution bestSolution) {
        PodRelocateMove best_move = null;
        double best_cost = data.big_num;
        for (int n = 0; n < sol.pods.size(); n++) {
            Pod pod = sol.pods.get(n);
            for (int i = 0; i < pod.segments.size(); i++) {
                Segment origin_seg = pod.segments.get(i);
                for (int j = 0; j < pod.segments.size(); j++) {
                    Segment target_seg = pod.segments.get(j);
                    if (i == j) continue;
                    for (int k = 1; k < origin_seg.route.size() - 1; k++) {
                        if (!pod.customers.get(origin_seg).contains(origin_seg.route.get(k))) continue;
                        for (int l = 1; l < target_seg.route.size() - 1; l++) {
                            PodRelocateMove new_move = new PodRelocateMove(data, n, i, j, k, l);
                            boolean is_feasible = new_move.evaluate(sol);
                            if (!is_feasible) continue;
                            ArrayList<TabuArc> arcList = new ArrayList<>();
                            arcList.add(new TabuArc(origin_seg.route.get(k - 1), origin_seg.route.get(k)));
                            arcList.add(new TabuArc(origin_seg.route.get(k), origin_seg.route.get(k + 1)));
                            arcList.add(new TabuArc(target_seg.route.get(l - 1), target_seg.route.get(l)));
                            boolean newMoveTabu = isTabu_re(arcList, iteration);
                            if (new_move.add_cost < best_cost) {
                                if (new_move.add_cost + sol.cost < bestSolution.cost || (!newMoveTabu)) {
                                    best_move = new_move;
                                    best_cost = new_move.add_cost;
                                }
                            }
                        }
                    }
                }
            }
        }
        return best_move;
    }

    public SolRelocateMove findBestSolReMove(Solution sol, int iteration, Solution bestSolution) {
        SolRelocateMove best_move = null;
        double best_cost = data.big_num;
        for (int n = 0; n < sol.pods.size(); n++) {
            Pod ori_pod = sol.pods.get(n);
            for (int m = 0; m < sol.pods.size(); m++) {
                Pod tar_pod = sol.pods.get(m);
                if (n == m) continue;
                for (int i = 0; i < ori_pod.segments.size(); i++) {
                    Segment origin_seg = ori_pod.segments.get(i);
                    for (int j = 0; j < tar_pod.segments.size(); j++) {
                        Segment target_seg = tar_pod.segments.get(j);
                        if (origin_seg.equals(target_seg)) continue;
                        for (int k = 1; k < origin_seg.route.size() - 1; k++) {
                            if (ori_pod.customers.get(origin_seg) != null && !ori_pod.customers.get(origin_seg).contains(origin_seg.route.get(k))) continue;
                            for (int l = 1; l < target_seg.route.size() - 1; l++) {
                                SolRelocateMove new_move = new SolRelocateMove(data, n, m, i, j, k, l);
                                boolean is_feasible = new_move.evaluate(sol);
                                if (!is_feasible) continue;
                                ArrayList<TabuArc> arcList = new ArrayList<>();
                                arcList.add(new TabuArc(origin_seg.route.get(k - 1), origin_seg.route.get(k)));
                                arcList.add(new TabuArc(origin_seg.route.get(k), origin_seg.route.get(k + 1)));
                                arcList.add(new TabuArc(target_seg.route.get(l - 1), target_seg.route.get(l)));
                                boolean newMoveTabu = isTabu_re(arcList, iteration);
                                if (new_move.add_cost < best_cost) {
                                    if (new_move.add_cost + sol.cost < bestSolution.cost || (!newMoveTabu)) {
                                        best_move = new_move;
                                        best_cost = new_move.add_cost;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return best_move;
    }

    public CWMove findBestCWMove(Solution sol, int iteration, Solution bestSolution) {
        CWMove best_move = null;
        double best_cost = data.big_num;
        // for one-pod-route
        for (int i = 0; i < sol.segments.size(); i++) {
            if (sol.segments.get(i).getStart() != 0 | sol.segments.get(i).getEnd() != 0)
                continue;
            // for partial route
            for (int j = 0; j < sol.segments.size(); j++) {
                if (i == j) continue;
                // COM
                CWMove new_CM_move = new CWMove(data, "CM", i, j);
                boolean is_feasible = new_CM_move.evaluate(sol);
                boolean newMoveTabu = isTabu(iteration);
                if (is_feasible && new_CM_move.add_cost < best_cost) {
                    if (new_CM_move.add_cost + sol.cost < bestSolution.cost || (!newMoveTabu)) {
                        best_cost = new_CM_move.add_cost;
                        best_move = new_CM_move;
                    }
                }
                // POM
                CWMove new_PM_move = new CWMove(data, "PM", i, j);
                is_feasible = new_PM_move.evaluate(sol);
                newMoveTabu = isTabu(iteration);
                if (is_feasible && new_PM_move.add_cost < best_cost) {
                    if (new_PM_move.add_cost + sol.cost < bestSolution.cost || (!newMoveTabu)) {
                        best_cost = new_PM_move.add_cost;
                        best_move = new_PM_move;
                    }
                }
            }
        }
        return best_move;
    }


    public boolean isTabu_re(ArrayList<TabuArc> arcList, int iteration) {
        for (TabuArc arc : arcList) {
            if (tabu_list_re[arc.from][arc.to] > iteration)
                return true;
        }
        return false;
    }

    public boolean isTabu_ex(ArrayList<TabuArc> arcList, int iteration) {
        for (TabuArc arc : arcList) {
            if (tabu_list_ex[arc.from][arc.to] > iteration)
                return true;
        }
        return false;
    }

    public boolean isTabu(int iteration) {
        if (tabu_list_mo > iteration)
            return true;
        return false;
    }
}