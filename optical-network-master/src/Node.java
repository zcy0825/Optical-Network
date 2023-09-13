/**
 * 节点类
 *
 * @author zcy
 * @date 2023/03/22
 */
public class Node {
    private int name;
    private int type;
    public Node(int name,int type) {
        this.name = name;
        this.type = type;
    }

    public void setName(int name){
        this.name = name;
    }
    public int getName() {
        return name;
    }
    public void setType(int type){
        this.type = type;
    }
    public int getType(){
        return this.type;
    }
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Node)) {
            return false;
        }
        Node other = (Node) obj;
        return name == other.getName();
    }
    public String toString(){
        StringBuilder str = new StringBuilder(" Node: " + this.name);
        return str.toString();
    }
}
