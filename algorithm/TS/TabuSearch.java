package TS;

import Common.Data;
import Common.Help;
import Common.Solution;
import Greedy.CWMove;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class TabuSearch {
    public Data data;
    public Help help;
    public Solution bestSolution;

    public TabuSearch(Data _data, Help _help, Solution sol) {
        data = _data;
        help = _help;
        bestSolution = new Solution(sol);
    }

    public void solve() throws IOException {
        double[] records = new double[data.tabu_iterations];
        Arrays.fill(records, 0);
        Solution solution = new Solution(bestSolution);
        MoveFinder moveFinder = new MoveFinder(data);
        Shaking shaking = new Shaking(data);
        int no_improve = 0;
        for (int i = 0; i < data.tabu_iterations; i++) {
            ArrayList<Move> moves = new ArrayList<>();

            SegRelocateMove bestSegReMove = moveFinder.findBestSegReMove(solution, i, bestSolution);
            if (bestSegReMove != null) moves.add(bestSegReMove);

            PodRelocateMove bestPodReMove = moveFinder.findBestPodReMove(solution, i, bestSolution);
            if (bestPodReMove != null) moves.add(bestPodReMove);

            SolRelocateMove bestSolReMove = moveFinder.findBestSolReMove(solution, i, bestSolution);
            if (bestSolReMove != null) moves.add(bestSolReMove);

            CWMove bestCWMove = moveFinder.findBestCWMove(solution, i, bestSolution);
            if (bestCWMove != null) moves.add(bestCWMove);

            moves.sort(new Comparator<Move>() {
                @Override
                public int compare(Move o1, Move o2) {
                    if (o1.add_cost - o2.add_cost > 0)
                        return 1;
                    else if (o1.add_cost == o2.add_cost)
                        return 0;
                    else
                        return -1;
                }
            });

            if (moves.isEmpty()) break;
            data.move_time.put(moves.get(0).type, data.move_time.get(moves.get(0).type) + 1);
            Move best_move = moves.get(0);
            if (best_move.type.endsWith("Re")) {
                best_move.applyMove(solution, moveFinder.tabu_list_re, i);
            } else if (best_move.type.endsWith("Ex")) {
                best_move.applyMove(solution, moveFinder.tabu_list_ex, i);
            } else {
                best_move.applyMove(solution, moveFinder.tabu_list_ex, i);
            }
            help.check_solution(solution);

            if (Help.doubleCompare(solution.cost, bestSolution.cost) == -1) {
                bestSolution = new Solution(solution);
                data.best_sol_iter = i;
                no_improve = 0;
                System.out.println("Iteration " + i + " - find a better cost: " + bestSolution.cost + ", " + moves.get(0).type);
            } else {
                no_improve++;
//                System.out.println("Iteration " + i + " - no improve" + ", " + moves.get(0).type);
            }
            if (no_improve > data.tabu_no_improve_iteration) {
                shaking.brokenPod(solution);
                System.out.println("breaking... " + " - now cost: " + solution.cost);
                help.check_solution(solution);
                no_improve = 0;
            }

            records[i] = bestSolution.cost;
        }

        // record
//        writeArrayToCSV(records, "converge-" + data.N + "-" + bestSolution.cost + ".csv");

        System.out.println("Best solution found at iteration " + data.best_sol_iter + ", Total Cost: " + bestSolution.cost);
    }

    public static void writeArrayToCSV(double[] array, String filePath) {
        try (FileWriter fileWriter = new FileWriter(filePath);
             PrintWriter printWriter = new PrintWriter(fileWriter)) {

            for (double number : array) {
                printWriter.println(number);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}