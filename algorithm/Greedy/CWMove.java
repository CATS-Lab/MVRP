package Greedy;

import Common.*;
import TS.Move;

import java.util.*;

public class CWMove extends Move {
    public int p1;  // one-pod-route
    public int p2;  // partial route
    public String insert_place_PM; // "H": insert p1 to the head of p2
    public int insert_place_CM;
    public int fpos, bpos;
    public boolean route_merging;

    // variables for CM move
    public ArrayList<Pair> pod_pairs;

    // variables for PM move
    public Path fpath;
    public Path bpath;

    public CWMove(Data data, String type, int p1, int p2) {
        this.data = data;
        this.type = type;
        this.p1 = p1;
        this.p2 = p2;
    }

    public ArrayList<Pair> pairing(Data data, Segment pr1, Segment pr2) {
        ArrayList<Pair> demand_pr1 = new ArrayList<>();
        for (int i = 0; i < pr1.related_pods.size(); i++)
            demand_pr1.add(new Pair(pr1.related_pods.get(i).demand, i));
        demand_pr1.sort(new Comparator<Pair>() {
            @Override
            public int compare(Pair p1, Pair p2) {
                return Double.compare(p2.a, p1.a);
            }
        });
        ArrayList<Pair> capacity_pr2 = new ArrayList<>();
        for (int i = 0; i < pr2.related_pods.size(); i++)
            capacity_pr2.add(new Pair(pr2.related_pods.get(i).demand, i));
        demand_pr1.sort(new Comparator<Pair>() {
            @Override
            public int compare(Pair p1, Pair p2) {
                return Double.compare(p1.a, p2.a);
            }
        });

        ArrayList<Pair> pairs = new ArrayList<>();
        for (int i = 0; i < demand_pr1.size(); i++) {
            for (int j = 0; j < capacity_pr2.size(); j++) {
                if (demand_pr1.get(i).a + capacity_pr2.get(j).a <= data.capacity) {
                    pairs.add(new Pair(demand_pr1.get(i).b, capacity_pr2.get(j).b));
                    capacity_pr2.remove(j);
                    break;
                }
            }
        }
        return pairs;
    }


    public boolean evaluate(Solution solution) {
        add_cost = 0;
        Segment s1 = solution.segments.get(p1);
        Segment s2 = solution.segments.get(p2);

//        if(s1.route.size()==3 && s1.route.get(1)==65&&s2.route.size()==4 && s2.route.get(1)==55&& s2.route.get(2)==40)
//            System.out.println();

        if (type.equals("CM")) {
            pod_pairs = pairing(data, s1, s2);
            if (s1.getPodsNum() != pod_pairs.size() || s2.getPodsNum() != pod_pairs.size()) // check pod num
                return false;

            double best_insert_cost = Double.MAX_VALUE;
            for (int i = 1; i < s2.route.size(); i++) {
                // check s1 time windows
                double time;
                if (i == 1) {
                    time = s2.start_time + data.dis[s2.route.get(0)][s1.route.get(1)];
                } else {
                    time = Math.max(s2.arrival_time.get(i - 2), data.ETW[s2.route.get(i - 1)]) + data.service_time[s2.route.get(i - 1)]
                            + data.dis[s2.route.get(i - 1)][s1.route.get(1)];
                }
                if (time > s1.latest_arrival_time.get(0))
                    continue;
                // check s2 time windows
                for (int j = 1; j < s1.route.size() - 2; j++) {
                    time = Math.max(time, data.ETW[s1.route.get(j)]);
                    time += data.service_time[s1.route.get(j)] + data.dis[s1.route.get(j)][s1.route.get(j + 1)];
                }
                time = Math.max(time, data.ETW[s1.route.get(s1.route.size() - 2)]);
                time += data.service_time[s1.route.get(s1.route.size() - 2)] + data.dis[s1.route.get(s1.route.size() - 2)][s2.route.get(i)];
                if (i != s2.route.size() - 1 || s2.serve_last_node)
                    if (time > s2.latest_arrival_time.get(i - 1))
                        continue;
                // check time windows for following segments
                for (int j = i; j < s2.route.size() - 1; j++) {
                    time = Math.max(time, data.ETW[s2.route.get(j)]);
                    time += data.service_time[s2.route.get(j)] + data.dis[s2.route.get(j)][s2.route.get(j + 1)];
                }
                if (s2.serve_last_node) {
                    time = Math.max(time, data.ETW[s2.route.get(s2.route.size() - 1)]);
                    time += data.service_time[s2.route.get(s2.route.size() - 1)];
                }
                if (!s2.checkBackSegTW(time))
                    continue;

                // update cost
                double insert_cost = -data.cost_para[s1.getPodsNum()] * data.dis[s2.route.get(i - 1)][s2.route.get(i)]
                        + data.cost_para[s1.getPodsNum()] * (data.dis[s2.route.get(i - 1)][s1.route.get(1)]
                        + data.dis[s1.route.get(s1.route.size() - 2)][s2.route.get(i)])
                        - data.cost_para[s1.getPodsNum()] * data.dis[0][s1.route.get(1)]
                        - data.cost_para[s1.getPodsNum()] * data.dis[s1.route.get(s1.route.size() - 2)][0];
                if (insert_cost < best_insert_cost) {
                    insert_place_CM = i;
                    best_insert_cost = insert_cost;
                }
            }
            if (best_insert_cost > Double.MAX_VALUE / 2)
                return false;

            add_cost += best_insert_cost;
        } else {
            if (s2.getStart() == 0 && s2.getEnd() == 0) {
                route_merging = true;

                if (s1.getPodsNum() + s2.getPodsNum() > data.L) return false;

                double min_fcost = data.big_num;
                int best_forward_place = 0;
                for (int i = 1; i < s2.route.size() - 1; i++) {
                    double time = s2.arrival_time.get(i - 1);
                    time = Math.max(time, data.ETW[s2.route.get(i)]);
                    time += data.service_time[s2.route.get(i)] + data.dis[s2.route.get(i)][s1.route.get(1)];
                    if (time > s1.latest_arrival_time.get(0))
                        continue;
                    double dis_s2i = 0;
                    for (int j = 0; j < i; j++)
                        dis_s2i += data.dis[s2.route.get(j)][s2.route.get(j + 1)];
                    double cost = (data.cost_para[s1.getPodsNum() + s2.getPodsNum()] - data.cost_para[s2.getPodsNum()]) * dis_s2i
                            + data.cost_para[s1.getPodsNum()] * data.dis[s2.route.get(i)][s1.route.get(1)];
                    if (cost < min_fcost) {
                        min_fcost = cost;
                        best_forward_place = i;
                    }
                }
                double min_bcost = data.big_num;
                int best_backward_place = 0;
                for (int i = 1; i < s2.route.size() - 1; i++) {
                    double time = Math.max(s1.arrival_time.get(s1.arrival_time.size() - 2), data.ETW[s1.route.get(s1.route.size() - 2)])
                            + data.service_time[s1.route.get(s1.route.size() - 2)] + data.dis[s1.route.get(s1.route.size() - 2)][s2.route.get(i)];
                    if (time > s2.latest_arrival_time.get(i - 1))
                        continue;
                    double dis_i2e = 0;
                    for (int j = i; j < s2.route.size() - 1; j++)
                        dis_i2e += data.dis[s2.route.get(j)][s2.route.get(j + 1)];
                    double cost = (data.cost_para[s1.getPodsNum() + s2.getPodsNum()] - data.cost_para[s2.getPodsNum()]) * dis_i2e
                            + data.cost_para[s1.getPodsNum()] * data.dis[s1.route.get(s1.route.size() - 2)][s2.route.get(i)];
                    if (cost < min_bcost) {
                        min_bcost = cost;
                        best_backward_place = i;
                    }
                }

                double min_pair_cost = data.big_num;
                int best_pair_forward_place = 0;
                int best_pair_backward_place = 0;
                for (int i = 1; i < s2.route.size() - 1; i++) {
                    for (int k = i + 1; k < s2.route.size() - 1; k++) {
                        double time = s2.arrival_time.get(i - 1);
                        time = Math.max(time, data.ETW[s2.route.get(i)]);
                        time += data.service_time[s2.route.get(i)] + data.dis[s2.route.get(i)][s1.route.get(1)];
                        if (time > s1.latest_arrival_time.get(0))
                            continue;
                        for (int j = 1; j < s1.route.size() - 2; j++) {
                            time = Math.max(time, data.ETW[s1.route.get(j)]);
                            time += data.service_time[s1.route.get(j)] + data.dis[s1.route.get(j)][s1.route.get(j + 1)];
                        }
                        time = Math.max(time, data.ETW[s1.route.get(s1.route.size() - 2)]);
                        time += data.service_time[s1.route.get(s1.route.size() - 2)] + data.dis[s1.route.get(s1.route.size() - 2)][s2.route.get(k)];
                        if (time > s2.latest_arrival_time.get(k - 1))
                            continue;

                        double dis_s2i2e = 0;
                        for (int j = 0; j < i; j++)
                            dis_s2i2e += data.dis[s2.route.get(j)][s2.route.get(j + 1)];
                        for (int j = k; j < s2.route.size() - 1; j++)
                            dis_s2i2e += data.dis[s2.route.get(j)][s2.route.get(j + 1)];
                        double cost = (data.cost_para[s1.getPodsNum() + s2.getPodsNum()] - data.cost_para[s2.getPodsNum()]) * dis_s2i2e
                                + data.cost_para[s1.getPodsNum()] * data.dis[s2.route.get(i)][s1.route.get(1)]
                                + data.cost_para[s1.getPodsNum()] * data.dis[s1.route.get(s1.route.size() - 2)][s2.route.get(k)];
                        if (cost < min_pair_cost) {
                            min_pair_cost = cost;
                            best_pair_forward_place = i;
                            best_pair_backward_place = k;
                        }
                    }
                }

                double forward_saving = data.cost_para[s1.getPodsNum()] * data.dis[0][s1.route.get(1)] - min_fcost;
                double backward_saving = data.cost_para[s1.getPodsNum()] * data.dis[s1.route.get(s1.route.size() - 2)][0] - min_bcost;
                double pair_saving = data.cost_para[s1.getPodsNum()] * data.dis[0][s1.route.get(1)]
                        + data.cost_para[s1.getPodsNum()] * data.dis[s1.route.get(s1.route.size() - 2)][0]
                        - min_pair_cost;

                if (best_pair_backward_place != 0 && best_pair_forward_place != 0 && pair_saving > forward_saving && pair_saving > backward_saving) {
                    insert_place_PM = "HT";
                    fpos = best_pair_forward_place;
                    bpos = best_pair_backward_place;
                    add_cost = -pair_saving;
                } else if (best_forward_place != 0 && forward_saving >= backward_saving) {
                    insert_place_PM = "H";
                    fpos = best_forward_place;
                    add_cost = -forward_saving;
                } else if (best_backward_place != 0) {
                    insert_place_PM = "T";
                    bpos = best_backward_place;
                    add_cost = -backward_saving;
                } else
                    return false;
            } else {
                route_merging = false;

                ArrayList<Path> forward_paths = solution.findForwardPath(s2, s1.getPodsNum());
                double min_fcost = data.big_num;
                Path best_forward_path = null;
                for (Path path : forward_paths) {
                    double cost = path.calculateAddPodCost(data);
                    if (cost < min_fcost) {
                        min_fcost = cost;
                        best_forward_path = path;
                    }
                }
                ArrayList<Path> backward_paths = solution.findBackwardPath(s2, s1.getPodsNum());
                double min_bcost = data.big_num;
                Path best_backward_path = null;
                for (Path path : backward_paths) {
                    double cost = path.calculateAddPodCost(data);
                    if (cost < min_bcost) {
                        min_bcost = cost;
                        best_backward_path = path;
                    }
                }

                if (best_forward_path == null && best_backward_path == null)
                    return false;

                double time;
                if (s2.route.get(0) != 0) { // check s1 time windows
                    time = s2.start_time + data.dis[s2.route.get(0)][s1.route.get(1)];
                    if (time > s1.latest_arrival_time.get(0))
                        return false;
                    for (int j = 1; j < s1.route.size() - 2; j++) {
                        time = Math.max(time, data.ETW[s1.route.get(j)]);
                        time += data.service_time[s1.route.get(j)] + data.dis[s1.route.get(j)][s1.route.get(j + 1)];
                    }
                    time = Math.max(time, data.ETW[s1.route.get(s1.route.size() - 2)]);
                    time += data.service_time[s1.route.get(s1.route.size() - 2)] + data.dis[s1.route.get(s1.route.size() - 2)][s2.getEnd()];
                } else { // check s2 time windows
                    time = s1.arrival_time.get(s1.arrival_time.size() - 1);
                }
                if (!s2.checkBackSegTW(time))
                    return false;

                double forward_saving = data.cost_para[s1.getPodsNum()] * data.dis[0][s1.route.get(1)]
                        - data.cost_para[s1.getPodsNum()] * data.dis[s2.route.get(0)][s1.route.get(1)] - min_fcost;
                double backward_saving = data.cost_para[s1.getPodsNum()] * data.dis[s1.route.get(s1.route.size() - 2)][0]
                        - data.cost_para[s1.getPodsNum()] * data.dis[s1.route.get(s1.route.size() - 2)][s2.route.get(s2.route.size() - 1)] - min_bcost;

                if (forward_saving > 0 && backward_saving > 0) {
                    insert_place_PM = "HT";
                    fpath = best_forward_path;
                    bpath = best_backward_path;
                    add_cost = -(forward_saving + backward_saving);
                } else if (forward_saving >= backward_saving) {
                    insert_place_PM = "H";
                    fpath = best_forward_path;
                    add_cost = -forward_saving;
                } else {
                    insert_place_PM = "T";
                    bpath = best_backward_path;
                    add_cost = -backward_saving;
                }
            }
        }

        return true;
    }


    public void applyMove(Solution solution) {
        Segment s1 = solution.segments.get(p1);
        Segment s2 = solution.segments.get(p2);
        if (type.equals("CM")) {
            s1.route.remove(0);
            s1.route.remove(s1.route.size() - 1);
            solution.segments.get(0).forward_segments.remove(s1);
            solution.segments.get(0).backward_segments.remove(s1);
            // update pod distance (minus)
            for (Pod pod : s2.related_pods) {
                pod.dis -= s2.dis;
            }
            s2.dis += s1.dis
                    - data.dis[s2.route.get(insert_place_CM - 1)][s2.route.get(insert_place_CM)]
                    - data.dis[0][s1.route.get(0)]
                    - data.dis[s1.route.get(s1.route.size() - 1)][0]
                    + data.dis[s2.route.get(insert_place_CM - 1)][s1.route.get(0)]
                    + data.dis[s1.route.get(s1.route.size() - 1)][s2.route.get(insert_place_CM)];
            // update segment
            s2.route.addAll(insert_place_CM, s1.route);
            // update pod distance (plus)
            for (Pod pod : s2.related_pods) {
                pod.dis += s2.dis;
            }
            // update paired pods information
            for (Pair pair : pod_pairs) {
                Pod pod1 = s1.related_pods.get((int) pair.a);
                Pod pod2 = s2.related_pods.get(pair.b);
                pod2.customers.get(s2).addAll(pod1.customers.get(s1));
                pod2.demand += pod1.demand;
                solution.pods.remove(pod1);
                s1.related_pods.remove(pod1);
            }
            // update unpaired pods information
            for (Pod pod1 : s1.related_pods) {
                pod1.segments.remove(s1);
                pod1.segments.add(pod1.segments.size() - 1, s2);
                HashSet<Integer> s1_cus = pod1.customers.remove(s1);
                pod1.customers.put(s2, s1_cus);
                s2.related_pods.add(pod1);
                pod1.dis = s2.dis;
                for (Segment s : fpath.routeList) {
                    pod1.segments.add(pod1.segments.size() - 1, s);
                    pod1.customers.put(s, new HashSet<>());
                    pod1.dis += s.dis;
                    s.related_pods.add(pod1);
                }
            }
            // update arrival time
            s2.updateBackSegArrivalTime(s2.start_time);
            s2.updateLatestArrivalTime();
            s2.updateSegmentStartTW();
            // update solution
            s2.demand += s1.demand;
            solution.cost += add_cost;
            solution.segments.remove(p1);
        } else if (type.equals("PM")) {
            if (route_merging) {
                // update pod distance (minus)
                for (Pod pod : s1.related_pods) {
                    pod.dis -= s1.dis;
                }
                if (insert_place_PM.equals("H")) {
                    // update s1
                    s1.route.remove(0);
                    s1.dis += -data.dis[0][s1.route.get(0)]
                            + data.dis[s2.route.get(fpos)][s1.route.get(0)];
                    s1.route.add(0, s2.route.get(fpos));
                    // create s3
                    ArrayList<Integer> forward_route = new ArrayList<>(s2.route.subList(0, fpos + 1));
                    Segment s3 = new Segment(forward_route, 0, true, data);
                    solution.segments.add(s3);
                    s3.forward_segments.add(solution.segments.get(0));
                    solution.segments.get(0).backward_segments.add(s3);
                    s3.backward_segments.add(s1);
                    s3.backward_segments.add(s2);
                    // update s2
                    s2.route.subList(0, fpos).clear();
                    s2.demand -= s3.demand;
                    s2.dis -= s3.dis;
                    s2.forward_segments = new HashSet<>();
                    s2.forward_segments.add(s3);
                    solution.segments.get(0).backward_segments.remove(s2);
                    s2.updateBackSegArrivalTime(s3.finish_time);
                    s2.updateLatestArrivalTime();
                    s2.updateSegmentStartTW();
                    // update s1
                    s1.forward_segments = new HashSet<>();
                    s1.forward_segments.add(s3);
                    solution.segments.get(0).backward_segments.remove(s1);
                    s1.updateBackSegArrivalTime(s3.finish_time);
                    s1.updateLatestArrivalTime();
                    s1.updateSegmentStartTW();
                    // update pod (include plus pod distance)
                    for (Pod pod : s1.related_pods) {
                        pod.segments.add(1, s3);
                        pod.customers.put(s3, new HashSet<>());
                        pod.dis += s1.dis + s3.dis;
                        s3.related_pods.add(pod);
                    }
                    for (Pod pod : s2.related_pods) {
                        HashSet<Integer> pod_and_s3 = new HashSet<>();
                        pod_and_s3.addAll(pod.customers.get(s2));
                        pod_and_s3.retainAll(s3.route);
                        pod.segments.add(1, s3);
                        pod.customers.get(s2).removeAll(pod_and_s3);
                        pod.customers.put(s3, pod_and_s3);
                        s3.related_pods.add(pod);
                    }
                } else if (insert_place_PM.equals("T")) {
                    // update s1
                    s1.route.remove(s1.route.size() - 1);
                    s1.dis += -data.dis[s1.route.get(s1.route.size() - 1)][0]
                            + data.dis[s2.route.get(0)][s1.route.get(0)]
                            + data.dis[s1.route.get(s1.route.size() - 1)][s2.route.get(bpos)];
                    s1.route.add(s2.route.get(bpos));
                    s1.updateBackSegArrivalTime(0);
                    s1.updateLatestArrivalTime();
                    s1.updateSegmentStartTW();
                    // update s2
                    ArrayList<Integer> backward_route = new ArrayList<>();
                    backward_route.addAll(s2.route.subList(bpos, s2.route.size()));
                    s2.route.subList(bpos + 1, s2.route.size()).clear();
                    s2.serve_last_node = true;
                    s2.updateBackSegArrivalTime(0);
                    s2.updateLatestArrivalTime();
                    s2.updateSegmentStartTW();
                    // create s3
                    double time = Math.max(s1.finish_time, s2.finish_time);
                    Segment s3 = new Segment(backward_route, time, false, data);
                    solution.segments.add(s3);
                    s3.forward_segments.add(s1);
                    s3.forward_segments.add(s2);
                    s3.backward_segments.add(solution.segments.get(0));
                    solution.segments.get(0).forward_segments.add(s3);
                    // update s2
                    s2.demand -= s3.demand;
                    s2.dis -= s3.dis;
                    s2.backward_segments = new HashSet<>();
                    s2.backward_segments.add(s3);
                    solution.segments.get(0).forward_segments.remove(s2);
                    // update s1
                    s1.serve_last_node = false;
                    s1.backward_segments = new HashSet<>();
                    s1.backward_segments.add(s3);
                    solution.segments.get(0).forward_segments.remove(s1);
                    // update pod (include plus pod distance)
                    for (Pod pod : s1.related_pods) {
                        pod.segments.add(pod.segments.size() - 1, s3);
                        pod.customers.put(s3, new HashSet<>());
                        pod.dis += s1.dis + s3.dis;
                        s3.related_pods.add(pod);
                    }
                    for (Pod pod : s2.related_pods) {
                        HashSet<Integer> pod_and_s3 = new HashSet<>();
                        pod_and_s3.addAll(pod.customers.get(s2));
                        pod_and_s3.retainAll(s3.route);
                        pod_and_s3.remove(s3.route.get(0));
                        pod.segments.add(pod.segments.size() - 1, s3);
                        pod.customers.get(s2).removeAll(pod_and_s3);
                        pod.customers.put(s3, pod_and_s3);
                        s3.related_pods.add(pod);
                    }
                } else {
                    // update s1
                    s1.route.remove(0);
                    s1.route.remove(s1.route.size() - 1);
                    s1.serve_last_node = false;
                    s1.dis += -data.dis[0][s1.route.get(0)]
                            - data.dis[s1.route.get(s1.route.size() - 1)][0]
                            + data.dis[s2.route.get(fpos)][s1.route.get(0)]
                            + data.dis[s1.route.get(s1.route.size() - 1)][s2.route.get(bpos)];
                    s1.route.add(0, s2.route.get(fpos));
                    s1.route.add(s2.route.get(bpos));
                    // create s3, s4
                    ArrayList<Integer> forward_route = new ArrayList<>();
                    forward_route.addAll(s2.route.subList(0, fpos + 1));
                    Segment s3 = new Segment(forward_route, 0, true, data);
                    solution.segments.add(s3);
                    s3.backward_segments.add(s1);
                    s3.backward_segments.add(s2);
                    s3.forward_segments.add(solution.segments.get(0));
                    solution.segments.get(0).backward_segments.add(s3);
                    // s1
                    s1.serve_last_node = false;
                    s1.updateBackSegArrivalTime(s3.finish_time);
                    s1.updateLatestArrivalTime();
                    s1.updateSegmentStartTW();
                    // s2
                    ArrayList<Integer> backward_route = new ArrayList<>();
                    backward_route.addAll(s2.route.subList(bpos, s2.route.size()));
                    s2.route.subList(bpos + 1, s2.route.size()).clear();
                    s2.route.subList(0, fpos).clear();
                    s2.serve_last_node = true;
                    s2.updateBackSegArrivalTime(s3.finish_time);
                    s2.updateLatestArrivalTime();
                    s2.updateSegmentStartTW();
                    // s4
                    double time = Math.max(s1.finish_time, s2.finish_time);
                    Segment s4 = new Segment(backward_route, time, false, data);
                    solution.segments.add(s4);
                    s4.forward_segments.add(s1);
                    s4.forward_segments.add(s2);
                    s4.backward_segments.add(solution.segments.get(0));
                    solution.segments.get(0).forward_segments.add(s4);
                    // update s2
                    s2.demand += -s3.demand - s4.demand;
                    s2.dis += -s3.dis - s4.dis;
                    s2.forward_segments = new HashSet<>();
                    s2.backward_segments = new HashSet<>();
                    solution.segments.get(0).forward_segments.remove(s2);
                    solution.segments.get(0).backward_segments.remove(s2);
                    s2.forward_segments.add(s3);
                    s2.backward_segments.add(s4);
                    // update s1
                    s1.forward_segments = new HashSet<>();
                    s1.backward_segments = new HashSet<>();
                    solution.segments.get(0).forward_segments.remove(s1);
                    solution.segments.get(0).backward_segments.remove(s1);
                    s1.forward_segments.add(s3);
                    s1.backward_segments.add(s4);
                    // update pod (include plus pod distance)
                    for (Pod pod : s1.related_pods) {
                        pod.segments.add(1, s3);
                        pod.customers.put(s3, new HashSet<>());
                        pod.segments.add(pod.segments.size() - 1, s4);
                        pod.customers.put(s4, new HashSet<>());
                        pod.dis += s1.dis + s3.dis + s4.dis;
                        s3.related_pods.add(pod);
                        s4.related_pods.add(pod);
                    }
                    for (Pod pod : s2.related_pods) {
                        HashSet<Integer> pod_and_s3 = new HashSet<>();
                        pod_and_s3.addAll(pod.customers.get(s2));
                        pod_and_s3.retainAll(s3.route);
                        pod.segments.add(1, s3);
                        pod.customers.get(s2).removeAll(pod_and_s3);
                        pod.customers.put(s3, pod_and_s3);

                        HashSet<Integer> pod_and_s4 = new HashSet<>();
                        pod_and_s4.addAll(pod.customers.get(s2));
                        pod_and_s4.retainAll(s4.route);
                        pod_and_s4.remove(s4.route.get(0));
                        pod.segments.add(pod.segments.size() - 1, s4);
                        pod.customers.get(s2).removeAll(pod_and_s4);
                        pod.customers.put(s4, pod_and_s4);
                        s3.related_pods.add(pod);
                        s4.related_pods.add(pod);
                    }
                }
            } else {
                // update pod distance (minus)
                for (Pod pod : s1.related_pods) {
                    pod.dis -= s1.dis;
                }
                if (insert_place_PM.equals("H")) {
                    // update s1
                    s1.route.remove(0);
                    s1.dis += -data.dis[0][s1.route.get(0)]
                            + data.dis[s2.route.get(0)][s1.route.get(0)];
                    s1.route.add(0, s2.route.get(0));
                    // update forward segments and s1 pod
                    s1.forward_segments = new HashSet<>();
                    s1.forward_segments.addAll(s2.forward_segments);
                    solution.segments.get(0).backward_segments.remove(s1);
                    for (Segment seg : s2.forward_segments) {
                        seg.backward_segments.add(s1);
                    }
                    for (Segment s : fpath.routeList) {
                        s.related_pods.addAll(s1.related_pods);
                        for (Pod pod : s1.related_pods) {
                            pod.segments.add(1, s);
                            pod.customers.put(s, new HashSet<>());
                            pod.dis += s.dis;
                        }
                    }
                    // update time
                    s1.updateBackSegArrivalTime(s2.start_time);
                } else if (insert_place_PM.equals("T")) {
                    // update s1
                    s1.route.remove(s1.route.size() - 1);
                    s1.dis += data.dis[s1.route.get(s1.route.size() - 1)][s2.route.get(s2.route.size() - 1)]
                            - data.dis[s1.route.get(s1.route.size() - 1)][0];
                    s1.route.add(s2.route.get(s2.route.size() - 1));
                    // update backward segments and s1 pod
                    s1.backward_segments = new HashSet<>();
                    s1.backward_segments.addAll(s2.backward_segments);
                    solution.segments.get(0).forward_segments.remove(s1);
                    for (Segment seg : s2.backward_segments) {
                        seg.forward_segments.add(s1);
                    }
                    for (Segment s : bpath.routeList) {
                        s.related_pods.addAll(s1.related_pods);
                        for (Pod pod : s1.related_pods) {
                            pod.segments.add(pod.segments.size() - 1, s);
                            pod.customers.put(s, new HashSet<>());
                            pod.dis += s.dis;
                        }
                    }
                    // update time
                    s1.updateBackSegArrivalTime(s1.start_time);
                } else {
                    // update s1
                    s1.route.remove(0);
                    s1.route.remove(s1.route.size() - 1);
                    s1.dis += data.dis[s2.route.get(0)][s1.route.get(0)]
                            - data.dis[0][s1.route.get(0)]
                            + data.dis[s1.route.get(s1.route.size() - 1)][s2.route.get(s2.route.size() - 1)]
                            - data.dis[s1.route.get(s1.route.size() - 1)][0];
                    s1.route.add(0, s2.route.get(0));
                    s1.route.add(s2.route.get(s2.route.size() - 1));
                    // update forward segments and s1 pod
                    s1.forward_segments = new HashSet<>();
                    s1.forward_segments.addAll(s2.forward_segments);
                    solution.segments.get(0).backward_segments.remove(s1);
                    for (Segment seg : s2.forward_segments) {
                        seg.backward_segments.add(s1);
                    }
                    for (Segment s : fpath.routeList) {
                        s.related_pods.addAll(s1.related_pods);
                        for (Pod pod : s1.related_pods) {
                            pod.segments.add(1, s);
                            pod.customers.put(s, new HashSet<>());
                            pod.dis += s.dis;
                        }
                    }
                    // update backward segments and s1 pod
                    s1.backward_segments = new HashSet<>();
                    s1.backward_segments.addAll(s2.backward_segments);
                    solution.segments.get(0).forward_segments.remove(s1);
                    for (Segment seg : s2.backward_segments) {
                        seg.forward_segments.add(s1);
                    }
                    for (Segment s : bpath.routeList) {
                        s.related_pods.addAll(s1.related_pods);
                        for (Pod pod : s1.related_pods) {
                            pod.segments.add(pod.segments.size() - 1, s);
                            pod.customers.put(s, new HashSet<>());
                            pod.dis += s.dis;
                        }
                    }
                    // update time
                    s1.updateBackSegArrivalTime(s2.start_time);
                }
                // update pod distance (plus)
                for (Pod pod : s1.related_pods) {
                    pod.dis += s1.dis;
                }
            }
            solution.cost += add_cost;
        }
    }

    @Override
    public void operateOn(Solution solution) {
        applyMove(solution);
    }

    @Override
    public void applyMove(Solution solution, int[][] tabu_list, int iteration) {
        applyMove(solution);
    }
}
