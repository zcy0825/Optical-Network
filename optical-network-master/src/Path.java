import java.util.ArrayList;
import java.util.Arrays;

/**
 * 存储最短路径信息（包含节点数组、链路数组和总长度）
 *
 * @author zcy
 * @date 2023/03/22
 */
public class Path {
    private ArrayList<Node> nodes;
    private ArrayList<Link> links;
    private int length;
    private final int GB = 1;
    public Path(ArrayList<Node> nodes,ArrayList<Link> links,int length){
        this.nodes = nodes;
        this.links = links;
        this.length = length;
    }
    public void setNodes(ArrayList<Node> nodes){
        this.nodes = nodes;
    }
    public ArrayList<Node> getNodes(){
        return this.nodes;
    }
    public void setLinks(ArrayList<Link> links){
        this.links = links;
    }
    public ArrayList<Link> getLinks(){
        return this.links;
    }

    /**
     * 获取当前路径上的所有空闲频谱块
     *
     * @return {@link ArrayList}<{@link int[]}>
     */
    public ArrayList<int[]> getAvailableSlots(ArrayList<candidatePath> path){
        int slotsNum = links.get(0).getSlots().size();
        int[] availableSlots = new int[slotsNum];
        Arrays.fill(availableSlots,0);

        for(Link link:links){
            int[] slots = new int[slotsNum];
            Arrays.fill(slots,0);
            for(int i=0;i<slotsNum;i++){
                // 链路频隙上业务的数量
                ArrayList<Service> services = link.getSlots().get(i);
                int serviceSize = services.size();
                // 如果有业务占用这个频隙，则不能再分配工作资源
                if(serviceSize != 0){
                    if(path != null && link.getBackupServices().contains(services.get(0)) && isLinkDisjoint(services,path)){
                        // 如果path != null，则说明是在寻找保护路径上的可用频隙
                        // 首先需要判断这个占用频隙的业务是否是在这条路径上分配了保护资源
                        // 再判断这个业务的工作路径和当前业务是否链路不相交
                        // 如果三个条件都满足，则当前频隙是保护路径上的可用频隙
                        break;
                    }else{
                        slots[i] = 1;
                    }
                }
            }

            // 链路上的源节点（即如在nodes中出现靠前的节点）是固定节点则链路是固定链路
            int indexOfNode1 = nodes.indexOf(link.getNode1());
            int indexOfNode2 = nodes.indexOf(link.getNode2());
            if(nodes.get(Math.min(indexOfNode1,indexOfNode2)).getType() == 1){
                link.setFixedLink(true);
                // 如果4个slot不是都可以用的，则用1填充全部的slot以表示不能再分配整4个slot了
                for(int i=0;i<slots.length;i+=4){
                    boolean isFreeSlot = true;
                    for(int j=i;j<i+4;j++){
                        if(slots[j] != 0){
                            isFreeSlot = false;
                            break;
                        }
                    }
                    if(!isFreeSlot){
                        for(int j=i;j<i+4;j++){
                            slots[j] = 1;
                        }
                    }
                }
            }else{
                link.setFixedLink(false);
            }
            // 路径上所有链路的slots信息叠在一起，最后剩下的频隙就是路径的可用频隙
            for (int i = 0; i < slots.length; i++) {
                if (slots[i] != 0) {
                    availableSlots[i] = slots[i];
                }
            }
        }
        // 返回频隙初始位置和空闲频隙长度构成的数组
        int i = 0;
        ArrayList<int[]> indexList = new ArrayList<>();
        while(i < availableSlots.length){
            if(availableSlots[i] == 0){
                int j = i + 1;
                while(j < availableSlots.length && availableSlots[j] == 0){
                    j++;
                }
                int[] indexMap = new int[]{0,0};
                if(i == 0){
                    indexMap[1] = j-GB;
                }else if(j == availableSlots.length){
                    indexMap[0] = i+GB;
                    indexMap[1] = j-i-GB;
                }else{
                    indexMap[0] = i+GB;
                    indexMap[1] = j-i-2*GB;
                }
                if(indexMap[1] > 0){
                    indexList.add(indexMap);
                }
                i = j + 1;
            }else{
                i++;
            }
        }
        return indexList;
    }
    public boolean isLinkDisjoint(ArrayList<Service> services,ArrayList<candidatePath> primaryPath){
        for(Service service:services){
            for(candidatePath path1:service.getPrimaryPath()){
                for(candidatePath path2:primaryPath){
                    for(Link link1:path1.getPath().getLinks()){
                        for(Link link2:path2.getPath().getLinks()){
                            if(link1.equals(link2)){
                                return false;
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    public void setLength(int length){
        this.length = length;
    }
    public int getLength(){
        return this.length;
    }
    public String toString(){
        StringBuilder str = new StringBuilder();
        for(Node node:nodes){
            str.append(node.getName() + " ");
        }
        str.append(" Length: " + length);
        return str.toString();
    }
}
