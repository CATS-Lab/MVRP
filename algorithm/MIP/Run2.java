package MIP;

import Common.*;
import ilog.concert.IloException;
import ilog.concert.IloNumVarType;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public class Run2 {
    public static void main(String[] args) throws IOException, IloException {
        String[] inst_name_list = {
                "C-n10-1", "C-n10-2", "C-n10-3", "C-n10-4", "C-n10-5",
        };
        for (int n = 0; n < inst_name_list.length; n++) {
            String inst_name = inst_name_list[n];
            String data_path = "./data/Small-size/" + inst_name + ".vrp";
            Input input = new Input();
            Data data = input.read_data(data_path, 'M');
            System.out.println("hello! " + inst_name);

            String MILP_file_path = "result/model/" + inst_name;
            double t1 = System.nanoTime();
            MIP model = new MIP(data, MILP_file_path);
            model.buildModel();
            Solution solution = model.solve();
            double t2 = System.nanoTime();

            Help help = new Help(data);
            help.check_solution(solution);

            String sum_file = "result/summery_MILP.csv";
            BufferedWriter sum_out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(sum_file, true)));
            sum_out.write(inst_name + "," + solution.cost + "," + (t2 - t1) / 1e9);
            sum_out.newLine();
            sum_out.close();

            help.output_solution(solution, inst_name, "model");
        }
    }
}
