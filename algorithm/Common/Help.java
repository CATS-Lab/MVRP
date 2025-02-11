package Common;

import java.io.*;
import java.util.*;

public class Help {
    Data data;

    public Help(Data d) {
        data = d;
    }

    public boolean check_solution(Solution solution) {
        // check pods
        for (Pod pod : solution.pods) {
            double dis = 0, demand = 0;
            for (int i = 0; i < pod.segments.size(); i++) {
                dis += pod.segments.get(i).dis;
            }
            for (Map.Entry<Segment, HashSet<Integer>> entry : pod.customers.entrySet()) {
                for (int cus : entry.getValue()) {
                    demand += data.demands[cus];
                    Segment seg = entry.getKey();
                    if (cus == 0) {
                        System.out.println("customer error>>" + demand);
//                        System.exit(0);
                        return true;
                    }
                    if (cus == seg.route.get(0)) {
                        System.out.println("customer error>>" + demand);
//                        System.exit(0);
                        return true;
                    }
                }
            }

            if (demand > data.capacity) {
                System.out.println("capacity exceed>>" + demand);
//                System.exit(0);
                return true;
            }
            if (demand != pod.demand) {
                System.out.println("pod demand error>>" + demand);
//                System.exit(0);
                return true;
            }

            if (demand == 0) {
                System.out.println("empty pod error>>");
//                System.exit(0);
                return true;
            }

            if (doubleCompare(dis, pod.dis) != 0) {
                System.out.println("pod distance error>>");
//                System.exit(0);
                return true;
            }

            int node = 0;
            for (int k = 0; k < pod.segments.size(); k++) {
                Segment seg = pod.segments.get(k);
                if (!seg.initial && pod.customers.get(seg)==null){
                        System.out.println("pod customer error>>");
//                System.exit(0);
                        return true;
                    }

                if (k==0 || k==pod.segments.size()-1){
                    if (!seg.initial){
                        System.out.println("pod segment initial error>>");
//                System.exit(0);
                        return true;
                    }

                }
                if (seg.getStart() != node){
                    System.out.println("pod segment sequence error>>");
//                System.exit(0);
                    return true;
                }
                node = seg.getEnd();
            }
        }

        for (Segment seg : solution.segments) {
            for (Segment forward_seg : seg.forward_segments) {
                if (forward_seg.getEnd() != seg.getStart()) {
                    System.out.println("segment error>>");
//                System.exit(0);
                    return true;
                }
            }
            for (Segment backward_seg : seg.backward_segments) {
                if (seg.getEnd() != backward_seg.getStart()) {
                    System.out.println("segment error>>");
//                System.exit(0);
                    return true;
                }
            }
        }

        // check segments
        double cost = 0;
        for (Segment seg : solution.segments) {
            if (seg.route.size() == 1)
                continue;
            double dis = 0;
            int demand = 0;
            double time = seg.start_time;
            for (int i = 1; i < seg.route.size(); i++) {
                if ((i < seg.route.size() - 1) || (i == seg.route.size() - 1 && seg.serve_last_node))
                    demand += data.demands[seg.route.get(i)];
                dis += data.dis[seg.route.get(i - 1)][seg.route.get(i)];
            }
            for (int i = 1; i < seg.route.size(); i++) {
                if (i > 1) {
                    time = Math.max(time, data.ETW[seg.route.get(i - 1)]);
                    time += data.service_time[seg.route.get(i - 1)] + data.dis[seg.route.get(i - 1)][seg.route.get(i)];
                } else {
                    time += data.dis[seg.route.get(0)][seg.route.get(1)];
                }
                if (i != seg.route.size() - 1 || seg.serve_last_node)
                    if (time > data.LTW[seg.route.get(i)]) {
                        System.out.println("time window error>>");
                        //                System.exit(0);
                        return true;
                    }
            }
            for (Segment seg_f : seg.forward_segments) {
                if (!solution.segments.contains(seg_f)) {
                    System.out.println("segment exist error>>");
//                System.exit(0);
                    return true;
                }
            }
            for (Segment seg_b : seg.backward_segments) {
                if (!solution.segments.contains(seg_b)) {
                    System.out.println("segment exist error>>");
//                System.exit(0);
                    return true;
                }
            }
            if (demand != seg.demand) {
                System.out.println("segment demand error>>" + demand);
//                System.exit(0);
                return true;
            }
            if (doubleCompare(dis, seg.dis) != 0) {
                System.out.println("segment distance error>>");
//                System.exit(0);
                return true;
            }
            cost += seg.dis * data.cost_para[seg.getPodsNum()];
        }

        if (doubleCompare(cost, solution.cost) != 0) {
            System.out.println("solution cost error>>");
//            System.exit(0);
            return true;
        }

        return false;
    }

    public void output_solution(Solution solution, String inst_name, String type) throws IOException {
        String sol_file = "result/" + type + "/" + inst_name + "_solution.txt";
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(sol_file)));
        out.write("Total cost:" + solution.cost + "\n");
        out.write("Segments:\n");
        for (Segment seg : solution.segments) {
            out.write(seg.toString() + "\n");
        }
        out.write("\nPods:\n");
        for (Pod pod : solution.pods) {
            out.write(pod.toString() + "\n");
        }
        out.newLine();
        out.close();
    }


    public static int doubleCompare(double a, double b) {
        if (a - b > 0.000001)
            return 1;
        if (b - a > 0.000001)
            return -1;
        return 0;
    }
}
