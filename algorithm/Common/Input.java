package Common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class Input {

    public Data read_data(String file_name, char type) throws IOException {
        Data data = new Data();
        File file = new File(file_name);
        FileReader fr = new FileReader(file);
        BufferedReader reader = new BufferedReader(fr);

        String line;
        String[] parts;
        line = reader.readLine();

        line = reader.readLine();
        parts = line.split("\\D+");
        data.N = Integer.parseInt(parts[1]);

        line = reader.readLine();
        parts = line.split("\\D+");
        data.Nv = Integer.parseInt(parts[1]);

        line = reader.readLine();
        parts = line.split("\\D+");
        data.capacity = Integer.parseInt(parts[1]);


        line = reader.readLine();
        parts = line.split("\\D+");
        data.L = Integer.parseInt(parts[1]);

        double discount = 0.05;
        data.cost_para = new double[data.L + 1];
        for (int i = 1; i <= data.L; i++)
            data.cost_para[i] = i * (1 - discount * (i - 1));


        line = reader.readLine();
        int[] x_cor = new int[data.N];
        int[] y_cor = new int[data.N];
        data.demands = new int[data.N];
        data.ETW = new int[data.N];
        data.LTW = new int[data.N];
        data.service_time = new int[data.N];
        for (int i = 0; i < data.N; i++) {
            if ((line = reader.readLine()) == null) {
                System.out.println("input error");
                System.exit(0);
            }
            parts = line.split("\\s+");
            x_cor[i] = Integer.parseInt(parts[2]);
            y_cor[i] = Integer.parseInt(parts[3]);
            data.demands[i] = Integer.parseInt(parts[4]);
            data.ETW[i] = Integer.parseInt(parts[5]);
            data.LTW[i] = Integer.parseInt(parts[6]);
            data.service_time[i] = Integer.parseInt(parts[7]);
        }
        data.LTW[0] = Integer.MAX_VALUE;

        data.dis = new double[data.N][data.N];
        for (int i = 0; i < data.N; i++) {
            for (int j = i + 1; j < data.N; j++) {
                if (i != j) {
                    if (type == 'M')
                        data.dis[i][j] = Math.abs(x_cor[i] - x_cor[j]) + Math.abs(y_cor[i] - y_cor[j]);
                    else if (type == 'E')
                        data.dis[i][j] = (double) Math.round(Math.sqrt(Math.pow(x_cor[i] - x_cor[j], 2) + Math.pow(y_cor[i] - y_cor[j], 2)) * 1000) /1000;
                    data.dis[j][i] = data.dis[i][j];
                }
            }
        }

        return data;
    }

}