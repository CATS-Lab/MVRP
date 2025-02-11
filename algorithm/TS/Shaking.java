package TS;

import Common.*;

import java.util.*;

public class Shaking {
    public Data data;

    public Shaking(Data data) {
        this.data = data;
    }

    public void brokenPod(Solution sol) {
        ArrayList<Integer> shaked_pods = new ArrayList<>();
        for (int i = 0; i < sol.pods.size(); i++) {
            if (sol.pods.get(i).segments.size() > 1) {
                boolean serve_first = false;
                for (Segment seg : sol.pods.get(i).segments) {
                    if (seg.initial) continue;
                    if (sol.pods.get(i).customers.get(seg).contains(seg.getStart())) {
                        serve_first = true;
                        break;
                    }
                }
                if (!serve_first && shaked_pods.size() < data.broken_num) shaked_pods.add(i);
            }
            if (shaked_pods.size() > data.broken_num) break;
        }
        Collections.sort(shaked_pods, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o2 - o1;
            }
        });
        for (int pod_id : shaked_pods) {
            Pod pod = sol.pods.get(pod_id);
            ArrayList<Integer> route = new ArrayList<>();
            route.add(0);
            for (Segment seg : pod.segments) {
                if (seg.initial) continue;
                seg.related_pods.remove(pod);
                if (seg.related_pods.isEmpty()) {
                    for (Segment seg_f : seg.forward_segments) {
                        seg_f.backward_segments.remove(seg);
                    }
                    for (Segment seg_b : seg.backward_segments) {
                        seg_b.forward_segments.remove(seg);
                    }
                    sol.segments.remove(seg);
                    for (int i = 0; i < seg.route.size(); i++){
                        if (pod.customers.get(seg).contains(seg.route.get(i))){
                            route.add(seg.route.get(i));
                        }
                    }
                } else {
                    for (int i = 0; i < seg.route.size(); i++){
                        if (pod.customers.get(seg).contains(seg.route.get(i))){
                            route.add(seg.route.get(i));
                        }
                    }
                    for (int i : pod.customers.get(seg)) {
                        seg.route.remove((Integer) i);
                    }
                }
            }
            sol.updateDAG();
            route.add(0);
            if (route.size() == 2) {
                sol.pods.remove(pod_id);
                continue;
            }
            Segment new_seg = new Segment(route, 0, false, data);
            new_seg.related_pods.add(pod);
            sol.segments.add(new_seg);
            sol.segments.get(0).forward_segments.add(new_seg);
            sol.segments.get(0).backward_segments.add(new_seg);
            pod.segments = new ArrayList<>();
            pod.segments.add(sol.segments.get(0));
            pod.segments.add(new_seg);
            pod.segments.add(sol.segments.get(0));
            pod.dis = new_seg.dis;
            pod.demand = new_seg.demand;
            pod.customers = new HashMap<>();
            HashSet<Integer> cus = new HashSet<>(new_seg.route);
            cus.remove(0);
            pod.customers.put(new_seg, cus);
        }
        for (Segment seg: sol.segments.get(0).backward_segments)
            seg.updateBackSegArrivalTime(0, true);
        sol.updateInfo(false);

        Help help = new Help(data);
        help.check_solution(sol);
//        System.out.println("shaking finish");
    }
}
