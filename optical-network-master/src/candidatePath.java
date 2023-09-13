import org.jetbrains.annotations.NotNull;

public class candidatePath {
    private Path path;
    private int startIndex; // 起始频隙索引值
    private int allocatedBandwidth; // 在这条路径上分配的带宽
    private int totalSlotsNum; // 占用的总频隙数
    public candidatePath(@NotNull Path path, int startIndex, int allocatedBandwidth){
        this.path = path;
        this.startIndex = startIndex;
        this.allocatedBandwidth = allocatedBandwidth;
        for(Link link: path.getLinks()){
            int[] indexMap = link.getIndexMap(startIndex,allocatedBandwidth);
            this.totalSlotsNum += indexMap[1];
        }
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
    }

    public int getAllocatedBandwidth() {
        return allocatedBandwidth;
    }

    public void setAllocatedBandwidth(int allocatedBandwidth) {
        this.allocatedBandwidth = allocatedBandwidth;
    }
    public int getTotalSlotsNum(){
        return this.totalSlotsNum;
    }

    @Override
    public String toString() {
        return "Path: " + path +
                ", startIndex = " + startIndex +
                ", allocatedBandwidth = " + allocatedBandwidth +
                ", totalSlotsNum = " + totalSlotsNum;
    }
}
