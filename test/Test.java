package test;

public class Test {

    public int findTheCity(int n, int[][] edges, int distanceThreshold) {
        int[][] edgeArr = new int[n][n];
        for (int i = 0; i < edgeArr.length; i++) {
            for (int j = 0; j < edgeArr.length; j++) {
                edgeArr[i][j] = (i == j) ? 0 : Integer.MAX_VALUE;
            }
        }
        for (int[] e : edges) {
            edgeArr[e[0]][e[1]] = e[2];
            edgeArr[e[1]][e[0]] = e[2];
        }
        for (int k = 0; k < edgeArr.length; k++) {
            for (int i = 0; i < edgeArr.length; i++) {
                for (int j = 0; j < edgeArr.length; j++) {
                    edgeArr[i][j] = Math.min(edgeArr[i][j],edgeArr[i][k] + edgeArr[k][j]);
                }
            }
        }
        int out = n;
        int outMin = n;
        for (int i = 0; i < edgeArr.length; i++) {
            int c = 0;
            for (int j = 0; j < edgeArr.length; j++) {
                if (edgeArr[i][j] <= distanceThreshold) {
                    c += 1;
                }
            }
            if (c <= outMin) {
                out = i;
                outMin = c;
            }
        }
        return out;
    }

    public static void main(String[] args) {
        System.out.println((int) Math.pow(10, 9)+7 == 1000000007);
    }

}
