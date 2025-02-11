package TS;

import Common.Data;
import Common.Solution;

import java.util.ArrayList;

public abstract class Move {
    public double add_cost;
    public String type;
    public Data data;

    public abstract void operateOn(Solution s);

    public abstract void applyMove(Solution solution, int[][] tabu_list, int iteration);

    public void updateTabuList(ArrayList<TabuArc> arc_list, int[][] tabu_list, int iteration) {
        for (TabuArc arc : arc_list) {
            tabu_list[arc.from][arc.to] = iteration + data.tabu_horizon;
        }
    }
}