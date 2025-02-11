package Greedy;

import Common.Data;
import Common.Solution;

import java.util.*;

public class Initial {
    public Data data;
    public PriorityQueue<Solution> sol_list;

    public Initial(Data _data) {
        data = _data;
        sol_list = new PriorityQueue<>(data.sol_num, new Comparator<Solution>() {
            @Override
            public int compare(Solution s1, Solution s2) {
                return Double.compare(s2.cost, s1.cost);
            }
        });
    }

    public void solve(Data data) {
        CWGreedy greedy = new CWGreedy(data);
        // ini VRP solutions
        ArrayList<String> types = new ArrayList<>();
        ArrayList<ArrayList<ArrayList<Integer>>> VRP_sol_list = getInitialSolutions(types);
        // CW optimize
        for (int i = 0; i < VRP_sol_list.size(); i++) {
            ArrayList<ArrayList<Integer>> vehicles = VRP_sol_list.get(i);
            Solution new_sol = greedy.solve(vehicles);
            new_sol.origin = types.get(i);
            sol_list.offer(new_sol);
            if (sol_list.size() > data.sol_num) {
                sol_list.poll();
            }
        }
    }

    public ArrayList<ArrayList<ArrayList<Integer>>> getInitialSolutions(ArrayList<String> types) {
        ArrayList<ArrayList<ArrayList<Integer>>> solutions = new ArrayList<>();

        solutions.addAll(getSparseVRPSolutions());
        while (solutions.size() > types.size())
            types.add("sparse");
        System.out.println("End sampling...");

        ArrayList<ArrayList<Integer>> vehicles = new ArrayList<>();
        for (int i = 1; i < data.N; i++) {
            ArrayList<Integer> vehicle = new ArrayList<>();
            vehicle.add(0);
            vehicle.add(i);
            vehicle.add(0);
            vehicles.add(vehicle);
        }
        solutions.add(vehicles);
        types.add("single");

        return solutions;
    }

    public ArrayList<ArrayList<ArrayList<Integer>>> getSparseVRPSolutions() {
        // type 2 average random
        data.sparse_vehicle_num *= data.Nv;
        ArrayList<ArrayList<ArrayList<Integer>>> solutions = new ArrayList<>();
        ContinueCW:
        for (int i = 0; i < data.cw_pop_num; i++) {
            ArrayList<Integer> numbers = new ArrayList<>();
            for (int j = 1; j < data.N; j++) {
                numbers.add(j);
            }
            Collections.shuffle(numbers, data.ran);

            ArrayList<ArrayList<Integer>> vehicles = new ArrayList<>();
            for (int j = 0; j < data.sparse_vehicle_num; j++) {
                vehicles.add(new ArrayList<>());
                vehicles.get(j).add(0);
            }
            int[] demands = new int[vehicles.size()];
            double[] time = new double[vehicles.size()];
            Arrays.fill(demands, 0);
            Arrays.fill(time, 0.0);
            for (int number : numbers) {
                int test = 0;
                while (true) {
                    test++;
                    if (test > 100)
                        continue ContinueCW;
                    double pro = data.ran.nextDouble();
                    int pos = (int) Math.floor(pro * vehicles.size());
                    ArrayList<Integer> vehicle = vehicles.get(pos);
                    if (time[pos] + data.dis[vehicle.get(vehicle.size() - 1)][number] < data.LTW[number] &&
                            demands[pos] + data.demands[number] <= data.capacity) {
                        demands[pos] += data.demands[number];
                        time[pos] += data.dis[vehicle.get(vehicle.size() - 1)][number];
                        time[pos] = Math.max(time[pos], data.ETW[number]) + data.service_time[number];
                        vehicle.add(number);
                        break;
                    }
                }
            }
            for (ArrayList<Integer> vehicle : vehicles) {
                vehicle.add(0);
            }
            vehicles.removeIf(integers -> integers.size() <= 2);
            solutions.add(vehicles);

            if (data.time_out()) break;
        }
        return solutions;
    }
}