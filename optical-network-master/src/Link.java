import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 链路类
 *
 * @author zcy
 * @date 2023/03/22
 */
public class Link {
    private Node node1; // 节点1
    private Node node2; // 节点2
    private int weight; // 链路长度（权值）
    private CopyOnWriteArrayList<Service> services; // 在这条链路上分配工作资源的业务
    private ArrayList<Service> backupServices; // 在这条链路上分配保护资源的业务
    private ArrayList<ArrayList<Service>> slots; // 链路上的频隙
    private final int slotNum = 400; // 链路上的频隙总数
    private boolean isFixedLink; // 用于判断是否是固定链路（每个业务创建的时候都设置一次isFixedLink的值）
    public Link(Node node1, Node node2, int weight) {
        this.node1 = node1;
        this.node2 = node2;
        this.weight = weight;
        services = new CopyOnWriteArrayList<>();
        backupServices = new ArrayList<>();
        slots = new ArrayList<>();
        for(int i=0;i<slotNum;i++){
            slots.add(new ArrayList<>());
        }
    }

    public Node getNode1() {
        return node1;
    }

    public Node getNode2() {
        return node2;
    }

    public int getWeight() {
        return weight;
    }

    /**
     * 获得链路上的slots数组
     *
     * @return {@link int[]}
     */
    public ArrayList<ArrayList<Service>> getSlots(){
        return this.slots;
    }

    /**
     * 给链路分配频隙，在slots数组中标记业务的id
     *
     * @param startIndex  开始索引
     * @param indexLength 索引长度
     * @param service     服务
     */
    public void setSlots(Service service,int startIndex,int indexLength){
        for(int i=startIndex;i<indexLength+startIndex && i<slots.size();i++){
            ArrayList<Service> slot = slots.get(i);
            slot.add(service);
            slots.set(i,slot);
        }
    }
    public void setEmptySlots(){
        slots = new ArrayList<>();
        for(int i=0;i<slotNum;i++){
            slots.add(new ArrayList<>());
        }
    }
    // 如果是固定链路可能需要调整一下分配的起始频隙和频隙长度
    public int[] getIndexMap(int startIndex, int allocatedBandwidth){
        int[] indexMap = new int[]{startIndex,allocatedBandwidth};
        int start1 = 0;
        int start2 = 0;
        int bandwidth1 = 0;
        int bandwidth2 = 0;
//        if(this.isFixedLink){
//            if(startIndex % 4 != 0){
//                startIndex -= startIndex % 4;
//                allocatedBandwidth += startIndex % 4;
//            }
//            if(allocatedBandwidth % 4 != 0){
//                allocatedBandwidth += 4 - allocatedBandwidth % 4;
//            }
//            indexMap[0] = startIndex;
//            indexMap[1] = allocatedBandwidth;
//        }
        if(this.isFixedLink){
            if(startIndex % 4 != 0){
                start1 = startIndex - startIndex % 4;
                start2 = startIndex + 4 - startIndex % 4;
                allocatedBandwidth += startIndex % 4;
            }
            if(allocatedBandwidth % 4 != 0){
                bandwidth1 = allocatedBandwidth + 4 - allocatedBandwidth % 4;
                bandwidth2 = allocatedBandwidth + 4 - allocatedBandwidth % 4;
            }
            if(bandwidth1 <= bandwidth2){
                indexMap[0] = start1;
                indexMap[1] = bandwidth1;
            }else{
                indexMap[0] = start2;
                indexMap[1] = bandwidth2;
            }
        }
        return indexMap;
    }
    public void setFixedLink(boolean fixedLink) {
        isFixedLink = fixedLink;
    }
    public void addService(Service service){
        services.add(service);
    }
    public void addBackupService(Service service){
        backupServices.add(service);
    }
    public void removeService(Service service){
        services.remove(service);
    }
    public void removeBackupService(Service service){
        backupServices.remove(service);
    }
    public CopyOnWriteArrayList<Service> getServices(){
        return this.services;
    }
    public ArrayList<Service> getBackupServices() {
        return this.backupServices;
    }

    public String toString(){
        return node1.getName() + "-->" + node2.getName() + ":  ";
    }
}
