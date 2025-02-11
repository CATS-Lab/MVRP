package Greedy;

import Common.*;

import java.util.ArrayList;

public class CWGreedy {
    public Data data;
    public Help help;

    public CWGreedy(Data _data) {
        data = _data;
        help = new Help(data);
    }

    public Solution solve(ArrayList<ArrayList<Integer>> vehicles) {
        Solution sol = new Solution(data);
        Segment depot_seg = new Segment(data);
        depot_seg.initial=true;
        sol.segments.add(depot_seg);
        for (ArrayList<Integer> vehicle : vehicles) {
            Segment seg = new Segment(vehicle, 0, false, data);
            depot_seg.backward_segments.add(seg);
            depot_seg.forward_segments.add(seg);
            seg.forward_segments.add(depot_seg);
            seg.backward_segments.add(depot_seg);
            sol.segments.add(seg);
            sol.pods.add(new Pod(depot_seg, seg, data));
            seg.related_pods.add(sol.pods.get(sol.pods.size() - 1));
            depot_seg.related_pods.add(sol.pods.get(sol.pods.size() - 1));
            sol.cost += data.cost_para[1] * sol.pods.get(sol.pods.size() - 1).dis;
        }
//        System.out.println("cost before CW: " + sol.cost);

        while (true) {
            double min_cost = 0;
            CWMove best_move = null;
            ArrayList<Segment> segments = sol.segments;

            // for one-pod-route
            for (int i = 0; i < segments.size(); i++) {
                if (segments.get(i).initial) continue;
                if (segments.get(i).getStart() != 0 || segments.get(i).getEnd() != 0) continue;
                // for partial route
                for (int j = 0; j < segments.size(); j++) {
                    if (i == j) continue;
                    if (segments.get(j).initial) continue;
                    // combined merge
                    CWMove new_CM_move = new CWMove(data, "CM", i, j);
                    boolean is_feasible = new_CM_move.evaluate(sol);
                    if (is_feasible && new_CM_move.add_cost < min_cost) {
                        min_cost = new_CM_move.add_cost;
                        best_move = new_CM_move;
                    }
                    // parallel merge
                    CWMove new_PM_move = new CWMove(data, "PM", i, j);
                    is_feasible = new_PM_move.evaluate(sol);
                    if (is_feasible && new_PM_move.add_cost < min_cost) {
                        min_cost = new_PM_move.add_cost;
                        best_move = new_PM_move;
                    }
                }
            }
            if (min_cost == 0) break;
//            System.out.println(best_move.type);
            best_move.applyMove(sol);
            boolean debug = help.check_solution(sol);
            if (debug) {
                System.exit(0);
            }
        }
//        System.out.println("cost after CW: " + sol.cost);

        return sol;
    }
}