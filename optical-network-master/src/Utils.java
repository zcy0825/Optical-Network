import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Utils {
    public static void main(String[] args) {
        ArrayList<ArrayList<int[]>> arr = new ArrayList<>();
        ArrayList<int[]> indexList1 = new ArrayList<>();
        indexList1.add(new int[]{3,5});
        indexList1.add(new int[]{11,4});
        indexList1.add(new int[]{22,2});
        arr.add(indexList1);
        ArrayList<int[]> indexList2 = new ArrayList<>();
//        indexList2.add(new int[]{0,24});
        indexList2.add(new int[]{0,3});
        indexList2.add(new int[]{8,2});
        indexList2.add(new int[]{14,3});
        indexList2.add(new int[]{20,4});
        arr.add(indexList2);
        ArrayList<int[]> indexList3 = new ArrayList<>();
        indexList3.add(new int[]{0,6});
        indexList3.add(new int[]{10,2});
        indexList3.add(new int[]{18,6});
        arr.add(indexList3);
        ArrayList<ArrayList<int[]>> numLists = getSortedNumLists(arr,3,6);
        for (ArrayList<int[]> numList : numLists) {
            for(int[] num:numList){
                System.out.print(Arrays.toString(num));
            }
            System.out.println();
        }
    }

    /**
     * 得到多条路径中最佳空闲频谱块组合
     *
     * @param arr       空闲频谱块数组
     * @param pathNum   路径个数
     * @param bandwidth 带宽需求
     * @return {@link ArrayList}<{@link ArrayList}<{@link int[]}>>
     */
    public static @NotNull ArrayList<ArrayList<int[]>> getSortedNumLists(ArrayList<ArrayList<int[]>> arr, int pathNum, int bandwidth) {
        ArrayList<ArrayList<int[]>> varLists = new ArrayList<>();
        ArrayList<ArrayList<int[]>> sumLists = new ArrayList<>();
        ArrayList<ArrayList<int[]>> numLists = new ArrayList<>();
        int[] indices = new int[pathNum];
        Arrays.fill(indices, 0);
        while (true) {
            ArrayList<int[]> numList = new ArrayList<>();
            int totalSlots = 0;
            for (int i = 0; i < pathNum; i++) {
                int[] indexMap = arr.get(i).get(indices[i]);
                numList.add(indexMap);
                totalSlots += indexMap[1];
            }
            if(totalSlots >= bandwidth){
                varLists.add(numList);
                sumLists.add(numList);
                numLists.add(numList);
            }
            int i = pathNum - 1;
            while (i >= 0 && indices[i] == arr.get(i).size() - 1) {
                i--;
            }
            if (i < 0) {
                break;
            }
            indices[i]++;
            for (int j = i + 1; j < pathNum; j++) {
                indices[j] = 0;
            }
        }
        // 按照方差大小的升序排序
        Collections.sort(varLists, new Comparator<ArrayList<int[]>>() {
            public int compare(ArrayList<int[]> a, ArrayList<int[]> b) {
                double varianceA = getVariance(a);
                double varianceB = getVariance(b);
                if (varianceA < varianceB) {
                    return -1;
                } else if (varianceA > varianceB) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
        // 按照频谱大小之和的升序排序
        Collections.sort(sumLists, new Comparator<ArrayList<int[]>>() {
            @Override
            public int compare(ArrayList<int[]> a, ArrayList<int[]> b) {
                double sumA = getSum(a);
                double sumB = getSum(b);
                if(sumA < sumB){
                    return -1;
                }else if(sumA > sumB){
                    return 1;
                }else{
                    return 0;
                }
            }
        });
        // 按照在前两个排序中出现位置之和的升序排序
        Collections.sort(numLists, new Comparator<ArrayList<int[]>>() {
            @Override
            public int compare(ArrayList<int[]> a, ArrayList<int[]> b) {
                int indexOfA = varLists.indexOf(a) + sumLists.indexOf(a);
                int indexOfB = varLists.indexOf(b) + sumLists.indexOf(b);
                if(indexOfA < indexOfB){
                    return -1;
                }else if(indexOfA > indexOfB){
                    return 1;
                }else{
                    return 0;
                }
            }
        });
        return numLists;
    }

    /**
     * 得到方差值
     *
     * @param arr 输入数组
     * @return double
     */
    public static double getVariance(ArrayList<int[]> arr) {
        double mean = getSum(arr) / (double) arr.size();
        double variance = 0;
        for (int[] num : arr) {
            variance += Math.pow(num[1] - mean, 2);
        }
        variance /= arr.size();
        return variance;
    }

    /**
     * 得到和
     *
     * @param arr 输入数组
     * @return double
     */
    public static double getSum(ArrayList<int[]> arr){
        double sum = 0;
        for (int[] num : arr) {
            sum += num[1];
        }
        return sum;
    }

    /**
     * 得到从源节点到目的节点的最短路径
     *
     * @param nodes    节点数组
     * @param links    链路数组
     * @param srcNode  源节点
     * @param destNode 目的节点
     * @return {@link ArrayList}<{@link Node}>
     */
    public static ArrayList<Node> getShortestPath(ArrayList<Node> nodes, ArrayList<Link> links, Node srcNode, Node destNode) {
        Map<Node, Integer> distance = new HashMap<>();
        Map<Node, Node> previous = new HashMap<>();
        Set<Node> visited = new HashSet<>();
        PriorityQueue<Node> pq = new PriorityQueue<>((n1, n2) -> distance.getOrDefault(n1, Integer.MAX_VALUE)
                - distance.getOrDefault(n2, Integer.MAX_VALUE));
        ArrayList<Node> shortestPath = new ArrayList<>();

        // initialization
        for (Node node : nodes) {
            distance.put(node, Integer.MAX_VALUE);
            previous.put(node, null);
        }
        distance.put(srcNode, 0);

        pq.offer(srcNode);

        while (!pq.isEmpty()) {
            Node current = pq.poll();

            if (current.equals(destNode)) {
                // build shortest path
                while (previous.get(current) != null) {
                    shortestPath.add(current);
                    current = previous.get(current);
                }
                shortestPath.add(srcNode);
                Collections.reverse(shortestPath);
                break;
            }

            visited.add(current);

            for (Link link : links) {
                if (link.getNode1().equals(current) && !visited.contains(link.getNode2())) {
                    int dist = distance.get(current) + link.getWeight();
                    if (dist < distance.get(link.getNode2())) {
                        distance.put(link.getNode2(), dist);
                        previous.put(link.getNode2(), current);
                        pq.offer(link.getNode2());
                    }
                } else if (link.getNode2().equals(current) && !visited.contains(link.getNode1())) {
                    int dist = distance.get(current) + link.getWeight();
                    if (dist < distance.get(link.getNode1())) {
                        distance.put(link.getNode1(), dist);
                        previous.put(link.getNode1(), current);
                        pq.offer(link.getNode1());
                    }
                }
            }
        }
        return shortestPath;
    }
}
