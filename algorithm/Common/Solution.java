package Common;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.PriorityQueue;


public class Solution {
    public Data data;
    public double cost;
    public ArrayList<Segment> segments;
    public ArrayList<Pod> pods;

    public String origin;


    public Solution(Data _data) {
        cost = 0;
        data = _data;
        segments = new ArrayList<>();
        pods = new ArrayList<>();
    }

    public Solution(Solution sol) {
        data = sol.data;
        cost = sol.cost;
        // deep copy segments
        segments = new ArrayList<>();
        for (Segment seg : sol.segments) {
            segments.add(new Segment(seg));
        }
        for (int i = 0; i < segments.size(); i++) {
            for (Segment seg_f : sol.segments.get(i).forward_segments) {
                for (Segment seg : segments) {
                    if (seg.equals(seg_f)) {
                        segments.get(i).forward_segments.add(seg);
                        break;
                    }
                }
            }
            for (Segment seg_b : sol.segments.get(i).backward_segments) {
                for (Segment seg : segments) {
                    if (seg.equals(seg_b)) {
                        segments.get(i).backward_segments.add(seg);
                        break;
                    }
                }
            }
        }
        // deep copy pods
        pods = new ArrayList<>();
        for (Pod pod : sol.pods) {
            pods.add(new Pod(pod));
            for (Segment pod_seg : pod.segments) {
                for (Segment seg : segments) {
                    if (seg.equals(pod_seg)) {
                        pods.get(pods.size() - 1).segments.add(seg);
                        seg.related_pods.add(pods.get(pods.size() - 1));
                        if (pod_seg.route.size() > 1) {
                            HashSet<Integer> customers;
                            if (pod.customers.get(pod_seg) == null)
                                customers = new HashSet<>();
                            else
                                customers = new HashSet<>(pod.customers.get(pod_seg));
                            pods.get(pods.size() - 1).customers.put(seg, customers);
                        }
                        break;
                    }
                }
            }
        }
    }

    public void mergeSolutions() {
        ArrayList<Integer> remove_list = new ArrayList<>();
        for (int i = 0; i < segments.size(); i++) {
            Segment s1 = segments.get(i);
            if (s1.route.size() == 2 && s1.getStart() == 0 && s1.getEnd() == 0){
                remove_list.add(i);
                continue;
            }
            for (int j = i + 1; j < segments.size(); j++) {
                Segment s2 = segments.get(j);
                if (s1.equals(s2)) {
                    remove_list.add(j);
                }
            }
        }
        remove_list.sort((a, b) -> b - a);
        for (int i : remove_list){
            Segment seg = segments.get(i);
            seg.forward_segments.remove(seg);
            seg.backward_segments.remove(seg);
            for (Pod pod : seg.related_pods)
                pod.segments.remove(seg);
            segments.remove(i);
        }


        remove_list = new ArrayList<>();
        for (int i = 0; i < pods.size(); i++) {
            Pod s1 = pods.get(i);
            int totalCount = 0;
            for (HashSet<Integer> set : s1.customers.values()) {
                totalCount += set.size();
            }
            if (s1.demand==0 || totalCount==0) {
                remove_list.add(i);
                continue;
            }
            for (int j = i + 1; j < pods.size(); j++) {
                Pod s2 = pods.get(j);
                if (s1.equals(s2)) {
                    remove_list.add(j);
                }
            }
        }
        remove_list.sort((a, b) -> b - a);
        for (int i : remove_list){
            for (Segment seg : pods.get(i).segments)
                seg.related_pods.remove(pods.get(i));
            pods.remove(i);
        }


        cost = 0;
        for (Segment segment : segments) {
            if (segment.initial)
                continue;
            cost += segment.dis * data.cost_para[segment.getPodsNum()];
        }

    }

    public Pod findEmptyRoute() {
        if (pods.size() < data.Nv) {
            Pod new_route = new Pod(data);
            pods.add(new_route);
            return new_route;
        } else {
            return null;
        }
    }

    public void forwardSearch(Segment start, Path currentPath, ArrayList<Path> allPaths, int added_pod_num) {
        currentPath.routeList.add(start);

        if (start.forward_segments.isEmpty() && currentPath.routeList.size() > 1) {
            Path new_path = new Path(currentPath);
            new_path.routeList.remove(0);
            allPaths.add(new_path);
            return;
        }

        for (Segment nextRoute : start.forward_segments) {
            if (nextRoute.getPodsNum() + added_pod_num <= data.L) {
                forwardSearch(nextRoute, new Path(currentPath), allPaths, added_pod_num);
            }
        }
    }

    public void backwardSearch(Segment start, Path currentPath, ArrayList<Path> allPaths, int added_pod_num) {
        currentPath.routeList.add(start);

        if (start.backward_segments.isEmpty() && currentPath.routeList.size() > 1) {
            Path new_path = new Path(currentPath);
            new_path.routeList.remove(0);
            allPaths.add(new_path);
            return;
        }

        for (Segment nextRoute : start.backward_segments) {
            if (nextRoute.getPodsNum() + added_pod_num <= data.L) {
                backwardSearch(nextRoute, new Path(currentPath), allPaths, added_pod_num);
            }
        }
    }

    public ArrayList<Path> findForwardPath(Segment start_pr, int added_pod_num) {
        ArrayList<Path> allPaths = new ArrayList<>();
        if (added_pod_num == 0) return allPaths;
        forwardSearch(start_pr, new Path(), allPaths, added_pod_num);
        return allPaths;
    }


    public ArrayList<Path> findBackwardPath(Segment start_pr, int added_pod_num) {
        ArrayList<Path> allPaths = new ArrayList<>();
        if (added_pod_num == 0) return allPaths;
        backwardSearch(start_pr, new Path(), allPaths, added_pod_num);
        return allPaths;
    }

    public void buildDAG() {
        PriorityQueue<Integer> begin_list = new PriorityQueue();
        for (int i = 0; i < segments.size(); i++) {
            if (segments.get(i).getStart() == 0) {
                begin_list.add(i);
            }
        }
        while (begin_list.size() > 0) {
            int before = (int) (begin_list.poll());
            for (int i = 0; i < segments.size(); i++) {
                if (segments.get(before).getEnd() == segments.get(i).getStart()) {
                    segments.get(before).backward_segments.add(segments.get(i));
                    segments.get(i).forward_segments.add(segments.get(before));
                    if (begin_list.contains(i) == false && segments.get(i).getEnd() != 0)
                        begin_list.add(i);
                }
            }
        }
    }

    public void updateDAG() {
        while (true) {
            boolean no_update = true;
            for (Segment seg_f : segments) {
                if (seg_f.initial) continue;
                if (seg_f.backward_segments.size() == 1) {
                    Iterator<Segment> iterator = seg_f.backward_segments.iterator();
                    Segment seg_b = iterator.next();
                    if (seg_b.initial) continue;
                    if (seg_b.forward_segments.size() == 1) {
                        // merge route
                        seg_b.route.remove(0);
                        seg_f.route.addAll(seg_b.route);
                        // add seg forward info
                        seg_f.backward_segments = seg_b.backward_segments;
                        for (Segment seg : seg_b.backward_segments) {
                            seg.forward_segments.remove(seg_b);
                            seg.forward_segments.add(seg_f);
                        }
                        for (Pod pod : seg_b.related_pods) {
                            HashSet<Integer> cus = pod.customers.get(seg_f);
                            cus.addAll(pod.customers.remove(seg_b));
                            pod.customers.put(seg_f, cus);
                            pod.segments.remove(seg_b);
                        }
                        // remove seg backward info
                        segments.remove(seg_b);
                        no_update = false;
                        break;
                    }
                }
            }
            if (no_update) break;
        }
    }


    public void updateInfo(Boolean check_cost) {
        for (int i = 0; i < segments.size(); i++) {
            segments.get(i).updateDemandDistance();
            segments.get(i).id = i;
        }
        for (int i = 0; i < pods.size(); i++) {
            pods.get(i).update_info();
            pods.get(i).id = i;
        }
        double new_cost = 0;
        for (Segment seg : segments) {
            if (seg.route.size() > 1)
                new_cost += seg.dis * data.cost_para[seg.getPodsNum()];
        }
        if (check_cost) {
            if (Help.doubleCompare(cost, new_cost) != 0) {
                System.out.println("cost error>>");
                System.exit(0);
            }
        } else {
            cost = new_cost;
        }
    }
}