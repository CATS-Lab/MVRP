package Common;

import java.util.*;

public class Data {
    //instance message
    public int N;         // number of nodes
    public double[][] dis;
    public int[] demands; // demand list for all customers
    public int[] ETW;
    public int[] LTW;
    public int[] service_time;
    public int reformulation_time = 0;
    public int Nv;        // number of MVs
    public int L;  // max number of MVs in one platoon
    public int capacity;// capacity for each MV
    public double[] cost_para; // cost per distance

    // CW parameters
    public int cw_pop_num = 20000;
    public double sparse_vehicle_num = 1; // * np
    public int sol_num = 100;
    public double cw_start_time = 0;
    public double total_threshold = 10; // termination criteria (s)

    // tabu parameters
    public int tabu_horizon = 20;
    public int tabu_iterations = 500;
    public int tabu_no_improve_iteration = 50;
    public int broken_num = 5;
    public int best_sol_iter = 0;
    public HashMap<String, Integer> move_time;


    // system parameters
    public Random ran;
    public int seed = 15;
    public double big_num = 1e10;

    public Data() {
        ran = new Random(seed);
        move_time = new HashMap<>();
        move_time.put("SegRe", 0);
        move_time.put("PodRe", 0);
        move_time.put("SolRe", 0);
        move_time.put("CM", 0);
        move_time.put("PM", 0);
    }

    public boolean time_out() {
        double t = System.nanoTime();
        if ((t - cw_start_time) / 1e9 > total_threshold)
            return true;
        else
            return false;
    }
}
