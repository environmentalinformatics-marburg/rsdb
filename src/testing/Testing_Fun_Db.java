package testing;

import pointdb.base.GeoPoint;
import pointdb.process.Fun_Db;
import util.collections.vec.Vec;

public class Testing_Fun_Db {

    public static void main(String[] args) {
        System.out.printf("%-12s | %-4s | %-14s%n", "Points", "Dim", "Box Dimension");
        System.out.println("-------------|------|----------------");

        printResult(generate1d(10), 1);
        printResult(generate1d(100), 1);
        printResult(generate1d(1_000), 1);
        printResult(generate1d(10_000), 1);
        printResult(generate1d(100_000), 1);
        printResult(generate1d(1_000_000), 1);
        printResult(generate1d(8_000_000), 1);

        System.out.println();

        printResult(generate2d(10), 2);
        printResult(generate2d(100), 2);
        printResult(generate2d(1_000), 2);
        printResult(generate2d(3_000), 2);

        System.out.println();

        printResult(generate3d(10), 3);
        printResult(generate3d(100), 3);
        printResult(generate3d(200), 3);
    }

    private static void printResult(Vec<GeoPoint> points, int dim) {
        double bd = Fun_Db.calc(points);
        String dimLabel = dim + "D";
        String bdStr = Double.isNaN(bd) ? "NaN" : String.format("%.4f", bd);
        
        System.out.printf("%,12d | %-4s | %-14s%n", points.size(), dimLabel, bdStr);
    }

    private static Vec<GeoPoint> generate1d(int n) {
        Vec<GeoPoint> vec = new Vec<>();
        for (int x = 0; x < n; x++) {
            vec.add(GeoPoint.of(x * 0.21 + 10, 10, 10));
        }
        return vec;
    }

    private static Vec<GeoPoint> generate2d(int n) {
        Vec<GeoPoint> vec = new Vec<>();
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                vec.add(GeoPoint.of(x * 0.21 + 10, y * 0.21 + 10, 10));
            }
        }
        return vec;
    }

    private static Vec<GeoPoint> generate3d(int n) {
        Vec<GeoPoint> vec = new Vec<>();
        for (int z = 0; z < n; z++) {
            for (int y = 0; y < n; y++) {
                for (int x = 0; x < n; x++) {
                    vec.add(GeoPoint.of(x * 0.21 + 10, y * 0.21 + 10, z * 0.21 + 10));
                }
            }
        }
        return vec;
    }
}