package Main;

import Common.*;
import Greedy.Initial;
import TS.TabuSearch;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;


public class Run {
    public static void main(String[] args) throws IOException {
        String[] inst_name_list = {
                "C-n10-1", "C-n10-2", "C-n10-3", "C-n10-4", "C-n10-5",
//                "C-n50-1", "C-n50-2", "C-n50-3", "C-n50-4", "C-n50-5",
        };
        for (int n = 0; n < inst_name_list.length; n++) {
            String inst_name = inst_name_list[n];
            String data_path = "./data/Small-size/" + inst_name + ".vrp";
//            String data_path = "./data/Large-size/" + inst_name + ".vrp";
            Input input = new Input();
            Data data = input.read_data(data_path, 'M');
            Help help = new Help(data);
            System.out.println("hello! " + inst_name);

            Initial inite = new Initial(data);

            double t1 = System.nanoTime();
            data.cw_start_time = t1;
            inite.solve(data);

            Solution bestSolution = null;
            while (!inite.sol_list.isEmpty()) {
                Solution solution = inite.sol_list.poll();
                help.check_solution(solution);
                System.out.println(solution.origin + ", Initial cost: " + solution.cost);

                TabuSearch tabu = new TabuSearch(data, help, solution);
                tabu.solve();

                solution = tabu.bestSolution;
                help.check_solution(solution);
                solution.updateInfo(true);

                if (bestSolution == null || solution.cost < bestSolution.cost)
                    bestSolution = new Solution(solution);
            }
            double t2 = System.nanoTime();

            String sum_file = "result/summery_algorithm.csv";
            BufferedWriter sum_out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(sum_file, true)));
            sum_out.write(inst_name + "," + bestSolution.cost + "," + (t2 - t1) / 1e9);
            sum_out.newLine();
            sum_out.close();

            help.output_solution(bestSolution, inst_name, "algorithm");
        }
    }
}