import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Main {
    public static void main(String[] args) {
        // 储存网络信息的文件路径
        String url = "src/COST-239.txt";
        // 创建Graph对象，其中包含网络信息
        Graph graph = new Graph(url);
        double trafficLoad = 50;
        boolean isMASP = true;
        double blockRate = 0;
        double backupRate = 0;
        double survivalRate = 0;
        double averageAllocatedSlots = 0;

        for(int j=0;j<1;j++){
            int numRun = 100;
            for(int i=0;i<numRun;i++){
                MASP masp = new MASP(graph,trafficLoad,isMASP);
                blockRate += masp.getBlockRate();
                backupRate += masp.getBackupRate();
                survivalRate += masp.getSurvivalRate();
                averageAllocatedSlots += masp.getAverageAllocatedSlots();
            System.out.println(i + " BlockRate: " + masp.getBlockRate() + " BackupRate: " + masp.getBackupRate() + " SurvivalRate: " + masp.getSurvivalRate() + " AverageAllocatedSLots: " + masp.getAverageAllocatedSlots());
            }
            blockRate /= numRun;
            backupRate /= numRun;
            survivalRate /= numRun;
            averageAllocatedSlots /= numRun;
            DecimalFormat df = new DecimalFormat("#.##");
            String formattedBlockRate = df.format(blockRate*100);
            String formattedBackupRate = df.format(backupRate*100);
            String formattedSurvivalRate = df.format(survivalRate*100);
            String formattedAverageAllocatedSlots = df.format(averageAllocatedSlots);
            System.out.println("Traffic Load: " + trafficLoad + " isMASP: " + isMASP + " BlockRate: " + formattedBlockRate + " BackupRate: " + formattedBackupRate + " SurvivalRate: " + formattedSurvivalRate + " AverageAllocatedSLots: " + formattedAverageAllocatedSlots);

            trafficLoad += 5;
        }
    }

    /**
     * 输出当前链路状态
     *
     * @param linkList 链路数组
     */
    public static void printLinkList(ArrayList<Link> linkList){
        for(Link link:linkList){
            System.out.print(link);
            for(ArrayList<Service> slot:link.getSlots()){
                if(slot.size() == 0){
                    System.out.print("0 ");
                }else if(link.getBackupServices().contains(slot.get(0))){
                    System.out.print("-" + slot.size() + " ");
                }else{
                    System.out.print(slot.get(0).getId() + " ");
                }
            }
            System.out.println();
        }
    }
}
