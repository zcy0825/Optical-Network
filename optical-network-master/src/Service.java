import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 业务类
 *
 * @author zcy
 * @date 2023/03/22
 */
public class Service {
    private int id; // 业务id
    private Node srcNode; // 源节点
    private Node destNode; // 目的节点
    private int bandwidth; // 需求带宽
    private double arrivalTime; // 业务到达时间
    private double duration; // 业务持续时间
    private ArrayList<candidatePath> primaryPath; // 工作路径
    private candidatePath backupPath; // 保护路径
    private int totalAllocatedSlots;
    private CopyOnWriteArrayList<Path> paths; // K条最短路径的集合
    public Service(int id,Node srcNode,Node destNode,int bandwidth,double arrivalTime,double duration){
        this.id = id;
        this.srcNode = srcNode;
        this.destNode = destNode;
        this.bandwidth = bandwidth;
        this.arrivalTime = arrivalTime;
        this.duration = duration;
    }
    public int getId(){
        return this.id;
    }
    public Node getSrcNode(){
        return this.srcNode;
    }
    public Node getDestNode(){
        return this.destNode;
    }

    public double getArrivalTime() {
        return arrivalTime;
    }

    public double getDuration() {
        return duration;
    }

    /**
     * 给业务设置K条最短路径
     *
     * @param graph 图
     */
    public int setPaths(@NotNull Graph graph){
        paths = new CopyOnWriteArrayList<>();
        ArrayList<Node> nodeList = graph.getNodeList();
        // 深拷贝链路信息（防止修改整体网络结构）
        ArrayList<Link> linkList = new ArrayList<>();
        int linkNum = graph.getLinkNum();
        for(int i=0;i<linkNum;i++){
            linkList.add(graph.getLinkList().get(i));
        }

        // K取源节点和目的节点度的最小值
        int sourceDegree = 0;
        int destnationDegree = 0;
        for(Link link:linkList){
            if(link.getNode1().equals(srcNode) || link.getNode2().equals(srcNode)){
                sourceDegree++;
            }
            if(link.getNode1().equals(destNode) || link.getNode2().equals(destNode)){
                destnationDegree++;
            }
        }
        int K = Math.min(sourceDegree, destnationDegree);

        // 得到K条最短路径（路径之间的链路不相交）
        for(int i=0;i<K;i++) {
            int length = 0;
            // 用Dijkstra算法获取从源节点到目的节点最短路径的节点数组
            ArrayList<Node> nodes = Utils.getShortestPath(nodeList, linkList, srcNode, destNode);
            if(nodes.size() == 0){
                break;
            }
            // links用于存储最短路径上的链路信息
            ArrayList<Link> links = new ArrayList<>();
            for (int j = 0; j < nodes.size() - 1; j++) {
                Link link = graph.getLink(nodes.get(j), nodes.get(j + 1));
                links.add(link);
                // 在linkList中删除当前链路
                linkList.remove(link);
                length += link.getWeight();
            }

            Path path = new Path(nodes,links,length);
            // 获得路径上的所有空闲频谱块
            ArrayList<int[]> availableSlots = path.getAvailableSlots(null);
            // 只有路径上有空闲频谱块的时候才会把path添加到paths数组中
            if(availableSlots.size() != 0){
                paths.add(path);
//                System.out.println("Path" + (paths.indexOf(path)+1) + ": " + path);
            }
        }

        // 没有可以分配的路径，则返回-1
        if(paths.size() == 0){
            return -1;
        }
        return 0;
    }

    public int[] FirstFit(@NotNull ArrayList<int[]> indexList){
        for(int[] indexMap:indexList){
            if(indexMap[1] >= bandwidth){
                return indexMap;
            }
        }
        return new int[]{0,0};
    }
    public int[] BestFit(@NotNull ArrayList<int[]> indexList,int bandwidth){
        // 如果没有满足带宽需求的频隙就返回数组{0,0}
        int[] indexMap = new int[]{0,0};
        int min = Integer.MAX_VALUE;
        for(int[] i:indexList){
            if(i[1] >= bandwidth && i[1] < min){
                min = i[1];
                indexMap = i;
            }
        }
        return indexMap;
    }

    /**
     * 单路径算法：总的带宽需求由一条路径来实现(Best Fit)，返回候选路径对象
     *
     * @return {@link candidatePath}
     */
    public candidatePath singlePathAlgorithm(@NotNull CopyOnWriteArrayList<Path> paths, int bandwidth){
        // 在当前paths中选择一条可以BestFit需求带宽的最短路径
        for(Path path:paths){
            ArrayList<int[]> availableSlots = path.getAvailableSlots(null);
            int[] indexMap = BestFit(availableSlots,bandwidth);
            if(indexMap[1] != 0){
                return new candidatePath(path,indexMap[0],bandwidth);
            }
        }
        return null;
    }

    /**
     * 多路径算法：将总的需求带宽由多条路径来分担，返回所有候选路径构成的数组
     *
     * @return {@link ArrayList}<{@link candidatePath}>
     */
    public ArrayList<candidatePath> multipathAlgorithm(){
        ArrayList<candidatePath> candidatePaths = new ArrayList<>();
        // 把所有路径上的可用频谱信息写成一个二维数组
        ArrayList<ArrayList<int[]>> availableSlotsList = new ArrayList<>();
        for(Path path:paths){
            ArrayList<int[]> availableSlots = path.getAvailableSlots(null);
            availableSlotsList.add(availableSlots);
        }

        // 设定路径个数的初始值是2
        int pathNum = 2;
        ArrayList<ArrayList<int[]>> numLists = new ArrayList<>();
        // 执行getSortedNumLists()函数，增加路径个数值，直到拿到结果
        while(pathNum <= paths.size() && numLists.size() == 0){
            numLists = Utils.getSortedNumLists(availableSlotsList,pathNum,bandwidth);
            pathNum++;
        }
        // 如果尝试完K条路径都没有可用结果，则直接返回一个空的candidatePaths
        if(numLists.size() == 0){
            return candidatePaths;
        }

        // 取第一个（最佳的）空闲频谱块组合（深拷贝）
        ArrayList<int[]> numList = new ArrayList<>();
        for(int[] num:numLists.get(0)){
            int[] indexMap = new int[2];
            indexMap[0] = num[0];
            indexMap[1] = num[1];
            numList.add(indexMap);
        }

        // 剩余需要分配的带宽
        int unallocatedBandwidth = bandwidth;
        // 从最小的空闲频谱块开始分配
        while(numList.size() != 0){
            int[] minSlot = numList.get(0);
            for(int[] num:numList){
                if(num[1] < minSlot[1]){
                    minSlot = num;
                }
            }
            Path path = paths.get(numList.indexOf(minSlot));
            // 如果空闲频谱块比剩下需要分配的带宽还大的话就直接分配剩余带宽
            if(minSlot[1] >= unallocatedBandwidth){
                candidatePaths.add(new candidatePath(path,minSlot[0],unallocatedBandwidth));
                break;
            }else{
                // 否则需要分配整个剩余的slot
                candidatePaths.add(new candidatePath(path,minSlot[0],minSlot[1]));
                unallocatedBandwidth -= minSlot[1];
            }
            minSlot[1] = Integer.MAX_VALUE;
        }
        return candidatePaths;
    }

    /**
     * 确定工作路径和保护路径并分配频谱
     */
    public int setSlots(){
        ArrayList<candidatePath> singlePrimaryPath = new ArrayList<>();
        singlePrimaryPath.add(singlePathAlgorithm(paths,bandwidth));
        // 单路径算法和多路径算法得到的保护路径
        candidatePath singleBackupPath = null;
        candidatePath multiBackupPath = null;
        // 单路径算法和多路径算法占用的总频隙数（工作路径+保护路径）
        int singleTotalSlots = 0;
        int multiTotalSlots = 0;

        if(singlePrimaryPath.get(0) != null){
            // singleTotalSlots中加上工作路径占用的总频隙数
            singleTotalSlots += singlePrimaryPath.get(0).getTotalSlotsNum();
            // 比singlePrimaryPath.getPath()更短的路径上一定没有可用空闲频谱块了，因此要从之后的的路径上找
            for(int i = paths.indexOf(singlePrimaryPath.get(0).getPath()) + 1; i<paths.size(); i++){
                Path path = paths.get(i);
                // 单路径算法中保护路径上分配的带宽和工作路径上分配的带宽相同
                int allocatedBandwidth = singlePrimaryPath.get(0).getAllocatedBandwidth();
                // 得到路径上所有可用的保护频隙
                ArrayList<int[]> backupSlots = path.getAvailableSlots(singlePrimaryPath);
                // 使用BestFit在当前路径上分配频谱
                int[] indexMap = BestFit(backupSlots,allocatedBandwidth);
                // 如果分配成功则使用当前保护路径
                if(indexMap[1] != 0){
                    singleBackupPath = new candidatePath(path,indexMap[0],allocatedBandwidth);
                    break;
                }
            }
            // singleTotalSlots中加上保护路径占用的总频隙数
            if(singleBackupPath != null){
                singleTotalSlots += singleBackupPath.getTotalSlotsNum();
            }
        }
//        System.out.println("singlePathAlgorithm:");
//        System.out.println("Primary Path: " + singlePrimaryPath.get(0));
//        System.out.println("Backup Path: " + singleBackupPath);

        ArrayList<candidatePath> multiPrimaryPath = new ArrayList<>();
        // 至少有两条路径才会执行多路径算法
        if(paths.size() >= 2){
//            System.out.println("multiPathAlgorithm:\nPrimary Path:");
            multiPrimaryPath = multipathAlgorithm();
            if(multiPrimaryPath.size() != 0){
                // 多路径算法中分配给保护路径的频谱资源是多条路径中分配较少资源的那一个
                int backupBandwidth = multiPrimaryPath.get(0).getAllocatedBandwidth();
                for(candidatePath path: multiPrimaryPath){
                    if(path.getAllocatedBandwidth() < backupBandwidth){
                        backupBandwidth = path.getAllocatedBandwidth();
                    }
                    multiTotalSlots += path.getTotalSlotsNum();
//                    System.out.println(path);
                }
                // 多路径算法分配频谱资源
                for(Path path:paths){
                    // 判断这条路径在工作路径集合multiPrimaryPath中有没有出现过
                    boolean existInMultiPrimaryPath = false;
                    for(candidatePath p:multiPrimaryPath){
                        if(path.equals(p.getPath())){
                            existInMultiPrimaryPath = true;
                            break;
                        }
                    }
                    // 如果没有出现过，则可以让这条路径做保护路径
                    if(!existInMultiPrimaryPath){
                        ArrayList<int[]> backupSlots = path.getAvailableSlots(multiPrimaryPath);
                        int[] indexMap = BestFit(backupSlots,backupBandwidth);
                        if(indexMap[1] != 0){
                            multiBackupPath = new candidatePath(path,indexMap[0],backupBandwidth);
                            break;
                        }
                    }
                }
                if(multiBackupPath != null){
                    multiTotalSlots += backupBandwidth;
                }
            }
//            System.out.println("Backup Path: " + multiBackupPath);
        }

        if(singlePrimaryPath.get(0) != null && multiPrimaryPath.size() == 0){
            return -1;
        }else if(singleBackupPath == null && multiBackupPath == null){
            return -2;
        }
        // 分配工作路径和保护路径
        // 如果单条路径可以满足需求带宽并且比多路径算法占用的总频谱数少，则使用单路径，否则使用多路径
        if(singleBackupPath != null && (multiBackupPath == null || singleTotalSlots <= multiTotalSlots)){
            primaryPath = singlePrimaryPath;
            backupPath = singleBackupPath;
            totalAllocatedSlots = singleTotalSlots;
            setSlotsOnPath(singlePrimaryPath.get(0),true);
            setSlotsOnPath(backupPath,false);
            return 0;
        }else {
            primaryPath = multiPrimaryPath;
            backupPath = multiBackupPath;
            totalAllocatedSlots = multiTotalSlots;
            for (candidatePath path : multiPrimaryPath) {
                setSlotsOnPath(path,true);
            }
            setSlotsOnPath(backupPath,false);
            return 0;
        }
    }
    public void setSlotsOnPath(@NotNull candidatePath path,boolean isPrimaryPath){
        for(Link link: path.getPath().getLinks()){
            int startIndex = path.getStartIndex();
            int allocatedBandwidth = path.getAllocatedBandwidth();
            int[] indexMap = link.getIndexMap(startIndex,allocatedBandwidth);
            link.setSlots(this, indexMap[0],indexMap[1]);
            if(isPrimaryPath){
                link.addService(this);
            }else {
                link.addBackupService(this);
            }
        }
    }
    public int setSlots_ShortestPath_FirstFit(){
        if(paths.size() >= 2){
            Path path1 = paths.get(0);
            primaryPath = new ArrayList<>();
            ArrayList<int[]> availableSlots = path1.getAvailableSlots(null);
            int[] indexMap1 = FirstFit(availableSlots);
            if(indexMap1[1] != 0){
                candidatePath candidatepath1 = new candidatePath(path1, indexMap1[0],bandwidth);
                setSlotsOnPath(candidatepath1,true);
                primaryPath.add(candidatepath1);
                Path path2 = paths.get(1);
                int[] indexMap2 = FirstFit(path2.getAvailableSlots(primaryPath));
                if(indexMap2[1] != 0){
                    candidatePath candidatepath2 = new candidatePath(path2, indexMap2[0],bandwidth);
                    setSlotsOnPath(candidatepath2,false);
                    backupPath = candidatepath2;
                    totalAllocatedSlots = candidatepath1.getTotalSlotsNum() + candidatepath2.getTotalSlotsNum();
                    return 0;
                }else{
                    return -2;
                }
            }else{
                return -1;
            }
        }
        return -1;
    }
    // 传统方法对比效果
    public int setSlots_ShortestPath_BestFit(){
        if(paths.size() >= 2){
            Path path1 = paths.get(0);
            primaryPath = new ArrayList<>();
            ArrayList<int[]> availableSlots = path1.getAvailableSlots(null);
            int[] indexMap1 = BestFit(availableSlots,bandwidth);
            if(indexMap1[1] != 0){
                candidatePath candidatepath1 = new candidatePath(path1, indexMap1[0],bandwidth);
                setSlotsOnPath(candidatepath1,true);
                primaryPath.add(candidatepath1);
                Path path2 = paths.get(1);
                int[] indexMap2 = BestFit(path2.getAvailableSlots(primaryPath),bandwidth);
                if(indexMap2[1] != 0){
                    candidatePath candidatepath2 = new candidatePath(path2, indexMap2[0],bandwidth);
                    setSlotsOnPath(candidatepath2,false);
                    backupPath = candidatepath2;
                    return 0;
                }else{
                    return -2;
                }
            }else{
                return -1;
            }
        }
        return -1;
    }

    /**
     * 设置业务过期，释放资源
     */
    public void setExpired(){
        // 释放工作路径上的资源
        for(candidatePath path:primaryPath){
            for(Link link:path.getPath().getLinks()){
                for(ArrayList<Service> slot:link.getSlots()){
                    if(slot.contains(this)){
                        slot.remove(this);
                    }
                }
                link.removeService(this);
            }
        }
        // 释放保护路径上的资源
        if(backupPath != null){
            for(Link link:backupPath.getPath().getLinks()){
                for(ArrayList<Service> slot:link.getSlots()){
                    if(slot.contains(this)){
                        slot.remove(this);
                    }
                }
                link.removeBackupService(this);
            }
        }
    }
    public ArrayList<candidatePath> getPrimaryPath(){
        return this.primaryPath;
    }
    public candidatePath getBackupPath() {
        return this.backupPath;
    }
    public void setBackupPath(candidatePath backupPath){
        this.backupPath = backupPath;
    }
    public void removeBackupPath(){
        this.backupPath = null;
    }

    /**
     * 获取给业务分配的K条最短路径
     *
     * @return {@link CopyOnWriteArrayList}<{@link Path}>
     */
    public CopyOnWriteArrayList<Path> getPaths(){
        return this.paths;
    }
    public int getTotalAllocatedSlots(){
        return this.totalAllocatedSlots;
    }
    public String toString(){
        StringBuilder str = new StringBuilder("Source Node: " + srcNode.getName() + " Destination Node: " + destNode.getName() + " Demand Bandwidth: " + bandwidth + " ArrivalTime: " + arrivalTime + " Duration: " + duration + "\n");
        str.append("Primary Path: \n");
        for(candidatePath path :primaryPath){
            str.append(path + "\n");
        }
        str.append("Backup Path: \n" + backupPath + "\n");
        return str.toString();
    }
}
