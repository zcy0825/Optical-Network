import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public class MASP {
    private Graph graph;
    private double trafficLoad;
    private double blockRate;
    private double backupRate;
    private double survivalRate;
    private double averageAllocatedSlots;
    public MASP(Graph graph,double trafficLoad,boolean isMASP){
        this.graph = graph;
        this.trafficLoad = trafficLoad;
        runMASP(trafficLoad,isMASP);
    }
    public void runMASP(double trafficLoad,boolean isMASP){
        ArrayList<Node> nodeList = graph.getNodeList();
        ArrayList<Link> linkList = graph.getLinkList();

        double arrivalTime = 0; // 业务的到达时间
        double duration = 0; // 业务的持续时间
        int serviceNum = 5000; // 业务总数
        int affectedServices=0,blockedServices=0;
        int blockedServiceNum = 0; // 被阻塞的业务的个数
        int totalAllocatedSlots = 0;

        CopyOnWriteArrayList<Service> services = new CopyOnWriteArrayList<>(); // 所有业务构成的数组
        double arrivalRate = trafficLoad/10.0; // 每秒到达的业务个数
        double serviceRate = 0.1; // 每秒服务的业务个数
        Random rand = new Random(); // 生成随机数

        for(int i=0;i<serviceNum;i++){
            double interarrivalTime = -Math.log(1 - rand.nextDouble()) / arrivalRate;
            arrivalTime = (i == 0 ? interarrivalTime : arrivalTime + interarrivalTime);
            duration = -Math.log(1 - rand.nextDouble()) / serviceRate;

            int sourceNode = rand.nextInt(nodeList.size()); // nodeNum > sourceNode >= 0
            int destNode = rand.nextInt(nodeList.size());
            // 如果生成的目的节点和源节点相同，则重新生成直到两个节点不同
            while(destNode == sourceNode){
                destNode = rand.nextInt(nodeList.size());
            }
            int bandwidth = rand.nextInt(10) + 1; // 8+1 > bandwidth >= 0+1

            // 业务id从1开始
            Service service = new Service(i+1,nodeList.get(sourceNode), nodeList.get(destNode), bandwidth, arrivalTime, duration);
            double currentTime = service.getArrivalTime();
//            System.out.println("--------------------------------------------------------------------------------------");
//            System.out.println("Service Id: " + (i+1) + " " + sourceNode + " -> " + destNode + ": " + bandwidth + " Current Time: " + currentTime);

            if(service.setPaths(graph) == 0 && ((isMASP && service.setSlots() == 0) || (!isMASP && service.setSlots_ShortestPath_FirstFit() == 0))){
                services.add(service);
                totalAllocatedSlots += service.getTotalAllocatedSlots();
//                System.out.println(service);
            }else{
                blockedServiceNum++;
            }


            // 检查之前的service是否过期
            for(int j=0;j<services.indexOf(service);j++){
                Service s = services.get(j);
                if(s.getArrivalTime() + s.getDuration() <= currentTime){
                    s.setExpired();
                    services.remove(s);
//                    System.out.println("Service " + s.getId() + " is expired.");
                }
            }
            if((i+1) % 150 == 0){
                // 随机选择发生故障的链路
                int linkNum = rand.nextInt(linkList.size());
                Link failureLink = linkList.get(linkNum);
                // 出现单链路故障
//                System.out.println("Link " + failureLink + "fails.");
                affectedServices += failureLink.getServices().size();
                blockedServices += graph.singleLinkFailure(failureLink);

                // 恢复单链路故障
                graph.repairDestroyedLink();
            }
        }
        // 当前状态下保护资源占比（用来作为保护资源的频隙占所有使用过频隙的比值）
        int allocatedSlots = 0;
        int backupSlots = 0;
        for(Link link:linkList){
            for(ArrayList<Service> slot:link.getSlots()){
                if(slot.size() != 0) {
                    if (link.getBackupServices().contains(slot.get(0))) {
                        backupSlots++;
                    }
                    allocatedSlots++;
                }
            }
        }
        blockRate = (double)(blockedServiceNum+blockedServices)/serviceNum;
        backupRate = (double)backupSlots/allocatedSlots;
        survivalRate = (double)(affectedServices-blockedServices)/affectedServices;
        averageAllocatedSlots = (double)totalAllocatedSlots/serviceNum;
    }

    public double getTrafficLoad() {
        return trafficLoad;
    }

    public double getBlockRate() {
        return blockRate;
    }

    public double getBackupRate() {
        return backupRate;
    }

    public double getSurvivalRate() {
        return survivalRate;
    }

    public double getAverageAllocatedSlots() {
        return averageAllocatedSlots;
    }
}
