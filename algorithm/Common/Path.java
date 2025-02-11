package Common;

import java.util.ArrayList;

public class Path {
    public ArrayList<Segment> routeList = new ArrayList<>();

    public Path() {
    }

    public Path(Path other) {
        this.routeList.addAll(other.routeList);
    }

    public double calculateAddPodCost(Data data) {
        double cost = 0;
        for (Segment seg : routeList) {
            cost += (data.cost_para[seg.getPodsNum() + 1] - data.cost_para[seg.getPodsNum()]) * seg.dis;
        }
        return cost;
    }

    @Override
    public String toString() {
        return routeList.toString();
    }
}
