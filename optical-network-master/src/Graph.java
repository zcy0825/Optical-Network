import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 网络拓扑结构
 *
 * @author zcy
 * @date 2023/03/22
 */
public class Graph {
    private ArrayList<Node> nodeList; // 节点数组
    private ArrayList<Link> linkList; // 链路数组
    private ArrayList<Node> flexNodeList; // 灵活节点数组
    private int nodeNum; // 节点数目
    private int linkNum; // 链路数目
    private int flexNodeNum; // 灵活节点的数目
    private Link destroyedLink; // 发生故障的链路
    public Graph(String url){
        nodeList = new ArrayList<>();
        linkList = new ArrayList<>();
        flexNodeList = new ArrayList<>();
        try{
            File file = new File(url);
            Scanner scanner = new Scanner(file);

            // 文件第一行的两个数字分别是节点总数和链路总数
            String firstLine = scanner.nextLine();
            nodeNum = Integer.parseInt(firstLine.split(" ")[0]);
            linkNum = Integer.parseInt(firstLine.split(" ")[1]);
            // 初始化nodeList数组:从0开始为节点命名；类型全部初始化为固定节点（type=1)
            for(int i=0;i<nodeNum;i++){
                nodeList.add(new Node(i,1));
            }

            // 随机选取几个灵活节点
            Random rand = new Random();
            flexNodeNum = nodeNum / 4;
            for(int i=0;i<flexNodeNum;i++){
                int flexNode = rand.nextInt(nodeNum);
                // 随机挑选几个不重复的节点作为灵活节点
                while(flexNodeList.contains(nodeList.get(flexNode))){
                    flexNode = rand.nextInt(nodeNum);
                }
                Node node = nodeList.get(flexNode);
                nodeList.get(flexNode).setType(0);
                flexNodeList.add(node);
            }

            // 读取文件中的链路信息
            while(scanner.hasNext()){
                String line = scanner.nextLine();
                int node1 = Integer.parseInt(line.split(" ")[0]);
                int node2 = Integer.parseInt(line.split(" ")[1]);
                int weight = Integer.parseInt(line.split(" ")[2]);
                linkList.add(new Link(nodeList.get(node1), nodeList.get(node2),weight));
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    public void setNodeList(ArrayList<Node> nodeList){
        this.nodeList = nodeList;
    }
    public ArrayList<Node> getNodeList(){
        return this.nodeList;
    }
    public void setLinkList(ArrayList<Link> linkList){
        this.linkList = linkList;
    }
    public ArrayList<Link> getLinkList(){
        return this.linkList;
    }

    /**
     * 根据两端的节点找到对应的链路
     *
     * @param node1 node1
     * @param node2 node2
     * @return {@link Link}
     */
    public Link getLink(Node node1,Node node2){
        for(Link link:linkList){
            if((link.getNode1().equals(node1) && link.getNode2().equals(node2)) || (link.getNode1().equals(node2) && link.getNode2().equals(node1))){
                return link;
            }
        }
        return null;
    }
    public int getNodeNum(){
        return this.nodeNum;
    }
    public int getLinkNum(){
        return this.linkNum;
    }
    public ArrayList<Node> getFlexNodeList(){
        return this.flexNodeList;
    }

    /**
     * 单链路故障
     *
     * @param link 链路对象
     */
    public int singleLinkFailure(Link link){
        // 保存受损链路，方便之后恢复
        destroyedLink = link;
        // 在linkList中删除受损链路
        linkList.remove(link);
        // 链路总数减1
        linkNum--;
        int blockedNum = 0;

        // 更新所有受影响业务
        CopyOnWriteArrayList<Service> services = link.getServices();
        for(Service service: services){
            // 找到受影响的工作路径
            candidatePath affectedPrimaryPath = null;
            ArrayList<candidatePath> primaryPath = service.getPrimaryPath();
            for(candidatePath path:primaryPath){
                if(path.getPath().getLinks().contains(link)){
                    affectedPrimaryPath = path;
                    break;
                }
            }

            assert affectedPrimaryPath != null;
            for(Link l:affectedPrimaryPath.getPath().getLinks()){
                // 在service列表中删除当前业务
                l.removeService(service);
                // 在slots频隙数组中删除当前业务
                ArrayList<ArrayList<Service>> slots = l.getSlots();
                for(ArrayList<Service> slot:slots){
                    if(slot.contains(service)){
                        slots.set(slots.indexOf(slot),new ArrayList<>());
                    }
                }
            }
            // 更新当前业务的工作路径数组
            primaryPath.remove(affectedPrimaryPath);
            candidatePath backupPath = service.getBackupPath();
            // 如果当前业务没有保护路径（之前的链路故障之后已经受损了），则该业务被阻塞
            if(backupPath == null){
                blockedNum++;
                break;
            }
            candidatePath newPrimaryPath = new candidatePath(backupPath.getPath(),backupPath.getStartIndex(),affectedPrimaryPath.getAllocatedBandwidth());
            primaryPath.add(newPrimaryPath);
            for(Link l:backupPath.getPath().getLinks()){
                l.removeBackupService(service);
                ArrayList<ArrayList<Service>> slots = l.getSlots();
                for(ArrayList<Service> slot:slots){
                    // 所有含有service的slot都要清空
                    if(slot.contains(service)){
                        slots.set(slots.indexOf(slot),new ArrayList<>());
                    }
                }
            }
            // 更新工作路径和保护路径
            service.setSlotsOnPath(newPrimaryPath,true);
            service.removeBackupPath();
//            // 更新当前路径上的保护路径
//            service.getPaths().remove(affectedPrimaryPath.getPath());
//            for(Path path: service.getPaths()){
//                // 判断这条路径在工作路径集合multiPrimaryPath中有没有出现过
//                boolean existInMultiPrimaryPath = false;
//                for(candidatePath p: service.getPrimaryPath()){
//                    if(path.equals(p.getPath())){
//                        existInMultiPrimaryPath = true;
//                        break;
//                    }
//                }
//                // 如果没有出现过，则可以让这条路径做保护路径
//                if(!existInMultiPrimaryPath){
//                    ArrayList<int[]> backupSlots = path.getAvailableSlots(service.getPrimaryPath());
//                    int[] indexMap = service.BestFit(backupSlots, backupPath.getAllocatedBandwidth());
//                    if(indexMap[1] != 0){
//                        candidatePath newBackupPath = new candidatePath(path,indexMap[0],backupPath.getAllocatedBandwidth());
//                        service.setBackupPath(newBackupPath);
//                        service.setSlotsOnPath(newBackupPath,false);
//                        break;
//                    }
//                }
//            }
        }
        return blockedNum;
    }
    public void repairDestroyedLink(){
        // 在linkList中添加受损链路
        linkList.add(destroyedLink);
        // 链路总数加1
        linkNum++;
        // 清空slot上的所有业务
        destroyedLink.setEmptySlots();
    }
}
