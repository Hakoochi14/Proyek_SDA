import java.util.*;

// Kelas untuk menyimpan metadata ruangan/titik di dalam gedung
class EvacuationNode {
    String id;
    String type; // "room", "corridor", "exit", "window", "stairs"
    int floor;

    public EvacuationNode(String id, String type, int floor) {
        this.id = id;
        this.type = type;
        this.floor = floor;
    }
}

// Kelas untuk menyimpan informasi jalur antar ruangan
class EvacuationEdge {
    String targetId;
    double distance;
    double infraCondition;

    public EvacuationEdge(String targetId, double distance, double infraCondition) {
        this.targetId = targetId;
        this.distance = distance;
        this.infraCondition = infraCondition;
    }
}

// Kelas pembantu untuk menampung hasil perhitungan Dijkstra
class DijkstraResult {
    Map<String, Double> distances;
    Map<String, String> previousNodes;

    public DijkstraResult(Map<String, Double> distances, Map<String, String> previousNodes) {
        this.distances = distances;
        this.previousNodes = previousNodes;
    }
}

// Kelas untuk menyimpan state pencarian pada PriorityQueue
class State implements Comparable<State> {
    String nodeId;
    double distance;

    public State(String nodeId, double distance) {
        this.nodeId = nodeId;
        this.distance = distance;
    }

    @Override
    public int compareTo(State other) {
        return Double.compare(this.distance, other.distance);
    }
}

// KELAS UTAMA
public class SourceCodeProjek {
    private Map<String, EvacuationNode> nodes = new HashMap<>();
    private Map<String, List<EvacuationEdge>> graph = new HashMap<>();
    private double damageConstant;

    public SourceCodeProjek(double damageConstant) {
        this.damageConstant = damageConstant;
    }

    // Menambahkan titik/vertex baru ke dalam gedung
    public void addNode(String id, String type, int floor) {
        nodes.put(id, new EvacuationNode(id, type, floor));
        graph.putIfAbsent(id, new ArrayList<>());
    }

    // Menambahkan jalur/edge antar titik
    public void addEdge(String u, String v, double distance, double infraCondition, boolean isBidirectional) {
        if (!graph.containsKey(u) || !graph.containsKey(v)) {
            System.out.println("Error: Salah satu node tidak ditemukan saat membuat edge " + u + " -> " + v);
            return;
        }
        graph.get(u).add(new EvacuationEdge(v, distance, infraCondition));
        if (isBidirectional) {
            graph.get(v).add(new EvacuationEdge(u, distance, infraCondition));
        }
    }

    // Mencari semua tetangga yang terhubung langsung dengan seluruh titik bencana (Radius Risiko n=1)
    private Set<String> getAllDisasterNeighbors(Set<String> disasterNodes) {
        Set<String> neighbors = new HashSet<>();
        for (String disasterNode : disasterNodes) {
            if (graph.containsKey(disasterNode)) {
                // Tetangga dari edge keluar
                for (EvacuationEdge edge : graph.get(disasterNode)) {
                    neighbors.add(edge.targetId);
                }
                // Tetangga dari edge masuk
                for (Map.Entry<String, List<EvacuationEdge>> entry : graph.entrySet()) {
                    for (EvacuationEdge edge : entry.getValue()) {
                        if (edge.targetId.equals(disasterNode)) {
                            neighbors.add(entry.getKey());
                        }
                    }
                }
            }
        }
        return neighbors;
    }

    // Algoritma Dijkstra dengan Pembobotan Rumus f(n) dan Penanganan Multi-Bencana
    private DijkstraResult findShortestPaths(String startNode, Set<String> disasterNodes) {
        Map<String, Double> distances = new HashMap<>();
        Map<String, String> previousNodes = new HashMap<>();
        PriorityQueue<State> pq = new PriorityQueue<>();

        // Inisialisasi awal semua jarak dengan Infinity
        for (String node : nodes.keySet()) {
            distances.put(node, Double.POSITIVE_INFINITY);
            previousNodes.put(node, null);
        }

        distances.put(startNode, 0.0);
        pq.add(new State(startNode, 0.0));

        // Ambil semua node yang bertetangga dengan bencana untuk penentuan n=1
        Set<String> disasterNeighbors = getAllDisasterNeighbors(disasterNodes);

        while (!pq.isEmpty()) {
            State current = pq.poll();
            String currentNode = current.nodeId;
            
            if (current.distance > distances.get(currentNode)) {
                continue;
            }

            for (EvacuationEdge edge : graph.get(currentNode)) {
                String neighbor = edge.targetId;

                // KONDISI 1: Jika jalur menyentuh langsung area bencana, maka jalur terputus (skip)
                if (disasterNodes.contains(currentNode) || disasterNodes.contains(neighbor)) {
                    continue; 
                }

                // KONDISI 2: Menentukan nilai risiko n (1 jika dekat dengan bencana, 0 jika aman)
                int n = (disasterNeighbors.contains(currentNode) || disasterNeighbors.contains(neighbor)) ? 1 : 0;

                // Hitung bobot berdasarkan Rumus: f(n) = jarak + kondisi infrastruktur + n(konstanta kerusakan)
                double edgeWeight = edge.distance + edge.infraCondition + (n * damageConstant);
                double totalDistance = distances.get(currentNode) + edgeWeight;

                if (totalDistance < distances.get(neighbor)) {
                    distances.put(neighbor, totalDistance);
                    previousNodes.put(neighbor, currentNode);
                    pq.add(new State(neighbor, totalDistance));
                }
            }
        }
        return new DijkstraResult(distances, previousNodes);
    }

    // Rekonstruksi rute dari titik keluar mundur ke posisi user
    private List<String> buildPath(Map<String, String> previousNodes, String target) {
        List<String> path = new ArrayList<>();
        String current = target;
        while (current != null) {
            path.add(current);
            current = previousNodes.get(current);
        }
        Collections.reverse(path);
        return path;
    }

    // Fungsi Utama Penentu Rute Evakuasi Berdasarkan Hierarki Skenario Keselamatan
    public void getEvacuationRoute(String currentPos, Set<String> disasterNodes) {
        if (!nodes.containsKey(currentPos)) {
            System.out.println("Posisi user (" + currentPos + ") tidak valid.");
            return;
        }

        System.out.println("=================================================");
        System.out.println("         SISTEM NAVIGASI EVAKUASI GEDUNG        ");
        System.out.println("=================================================");
        System.out.println("Posisi Anda   : " + currentPos + " (Lantai " + nodes.get(currentPos).floor + ")");
        System.out.println("Titik Bencana : " + disasterNodes.toString());
        System.out.println("-------------------------------------------------");

        DijkstraResult result = findShortestPaths(currentPos, disasterNodes);
        Map<String, Double> distances = result.distances;
        Map<String, String> previousNodes = result.previousNodes;

        String bestTarget = null;
        double minDistance = Double.POSITIVE_INFINITY;
        String scenarioMsg = "";

        // SKENARIO A: Cari Pintu Exit terdekat yang masih bisa diakses
        for (EvacuationNode node : nodes.values()) {
            if (node.type.equals("exit")) {
                if (distances.get(node.id) < minDistance) {
                    minDistance = distances.get(node.id);
                    bestTarget = node.id;
                    scenarioMsg = "SKENARIO UTAMA: Jalur aman menuju Pintu Exit ditemukan.";
                }
            }
        }

        // SKENARIO B: Jika Exit terputus, arahkan ke Jendela Lantai 1 dengan bobot terendah
        if (bestTarget == null || minDistance == Double.POSITIVE_INFINITY) {
            System.out.println("[!] Peringatan: Jalur ke Pintu Exit terputus total!");
            System.out.println("[!] Mencari alternatif Jendela di Lantai 1...");
            for (EvacuationNode node : nodes.values()) {
                if (node.type.equals("window") && node.floor == 1) {
                    if (distances.get(node.id) < minDistance) {
                        minDistance = distances.get(node.id);
                        bestTarget = node.id;
                        scenarioMsg = "SKENARIO ALTERNATIF 1: Keluar lewat Jendela Lantai 1 terdekat.";
                    }
                }
            }
        }

        // SKENARIO C: Jika semua ruangan Lantai 1 tidak aktif, arahkan ke Jendela Ujung Koridor Lantai 2-4
        if (bestTarget == null || minDistance == Double.POSITIVE_INFINITY) {
            System.out.println("[!] Peringatan: Akses evakuasi Lantai 1 lumpuh total!");
            System.out.println("[!] Mencari Jendela Darurat di Ujung Koridor Lantai Atas (Lantai 2-4)...");
            for (EvacuationNode node : nodes.values()) {
                if (node.type.equals("window") && node.floor >= 2 && node.floor <= 4) {
                    if (distances.get(node.id) < minDistance) {
                        minDistance = distances.get(node.id);
                        bestTarget = node.id;
                        scenarioMsg = "SKENARIO ALTERNATIF 2: Menuju Jendela Koridor Lantai Atas.";
                    }
                }
            }
        }

        // Cetak Output Akhir Ke User
        if (bestTarget != null && minDistance != Double.POSITIVE_INFINITY) {
            System.out.println("\n[HASIL REKOMENDASI]");
            System.out.println("Status         : " + scenarioMsg);
            List<String> path = buildPath(previousNodes, bestTarget);
            System.out.println("Rute Evakuasi  : " + String.join(" -> ", path));
            System.out.println("Total Nilai f(n): " + minDistance);
        } else {
            System.out.println("\n[!] STATUS KRITIS: Semua jalur evakuasi terisolasi oleh bencana! Tetap di tempat dan cari perlindungan.");
        }
        System.out.println("=================================================\n");
    }

    // SIMULASI EKSEKUSI PROGRAM
    public static void main(String[] args) {
        // Set konstanta kerusakan kerusakan = 50.0
        SourceCodeProjek gedung = new SourceCodeProjek(50.0); 

        // 1. Menambahkan Vertex (Nodes)
        // Lantai 1
        gedung.addNode("1115", "room", 1);
        gedung.addNode("1117", "room", 1);
        gedung.addNode("1118", "room", 1);
        gedung.addNode("R.Rapat", "room", 1);
        gedung.addNode("R.Senat", "room", 1);
        gedung.addNode("R.Listrik", "room", 1);
        gedung.addNode("R.Laktasi", "room", 1);
        gedung.addNode("Toilet_L1", "room", 1);
        gedung.addNode("Toilet_Disabilitas_L1", "room", 1);
        gedung.addNode("R.Staff", "room", 1);
        gedung.addNode("Musala_L1", "room", 1);
        gedung.addNode("1121", "room", 1);
        gedung.addNode("1122", "room", 1);
        gedung.addNode("1123", "room", 1);
        gedung.addNode("1107", "room", 1);
        gedung.addNode("1108", "room", 1);
        gedung.addNode("Koridor_1115_L1", "corridor", 1);
        gedung.addNode("Koridor_1118_L1", "corridor", 1);
        gedung.addNode("Koridor_Tengah_L1", "corridor", 1);
        gedung.addNode("Koridor_Belakang_L1", "corridor", 1);
        gedung.addNode("Koridor_Musala_L1", "corridor", 1);
        gedung.addNode("Koridor_1121_L1", "corridor", 1);
        gedung.addNode("Koridor_1123_L1", "corridor", 1);
        gedung.addNode("Koridor_Depan_L1", "corridor", 1);
        gedung.addNode("Exit_Selatan", "exit", 1);
        gedung.addNode("Exit_Utara", "exit", 1);
        gedung.addNode("Exit_Depan", "exit", 1);
        gedung.addNode("Exit_Belakang", "exit", 1);
        gedung.addNode("Jendela_R.Rapat_L1", "window", 1);
        gedung.addNode("Jendela_1108_L1", "window", 1);
        gedung.addNode("Jendela_1107_L1", "window", 1);
        gedung.addNode("Tangga_Depan_L1", "stairs", 1);
        gedung.addNode("Tangga_Belakang_L1", "stairs", 1);

        // Menambahkan Edges
        gedung.addEdge("Exit_Selatan", "Koridor_1115_L1", 2.5, 0, true);
        gedung.addEdge("1115", "Koridor_1115_L1", 1, 0, true);
        gedung.addEdge("R.Rapat", "Koridor_1115_L1", 1, 10, true);
        gedung.addEdge("R.Rapat", "Jendela_R.Rapat_L1", 1, 0, true);
        gedung.addEdge("1117", "1118", 8, 0, true);
        gedung.addEdge("Koridor_1118_L1", "1118", 4, 0, true);
        gedung.addEdge("Koridor_1118_L1", "Koridor_1115_L1", 10, 0, true);
        gedung.addEdge("Koridor_1118_L1", "R.Senat", 1, 0, true);
        gedung.addEdge("Koridor_1118_L1", "R.Rapat", 1, 10, true);
        gedung.addEdge("Koridor_1118_L1", "Koridor_Tengah_L1", 1, 0, true);
        gedung.addEdge("Koridor_Depan_L1", "Koridor_Tengah_L1", 10, 3, true);
        gedung.addEdge("Koridor_Depan_L1", "R.Laktasi", 2, 0, true);
        gedung.addEdge("Koridor_Depan_L1", "Tangga_Depan_L1", 2, 5, true);
        gedung.addEdge("Koridor_Depan_L1", "Exit_Depan", 2, 0, true);
        gedung.addEdge("Koridor_Depan_L1", "R.Listrik", 2, 5, true);
        gedung.addEdge("Koridor_Belakang_L1", "Koridor_Tengah_L1", 6, 0, true);
        gedung.addEdge("Koridor_Belakang_L1", "Toilet_Disabilitas_L1", 2, 0, true);
        gedung.addEdge("Koridor_Belakang_L1", "Toilet_L1", 6, 0, true);
        gedung.addEdge("Koridor_Belakang_L1", "Tangga_Belakang_L1", 1, 0, true);
        gedung.addEdge("Koridor_Belakang_L1", "R.Staff", 1, 0, true);
        gedung.addEdge("Exit_Belakang", "Tangga_Belakang_L1", 1, 0, true);
        gedung.addEdge("Koridor_Musala_L1", "Koridor_Tengah_L1", 1, 0, true);
        gedung.addEdge("Koridor_Musala_L1", "Koridor_1121_L1", 5, 0, true);
        gedung.addEdge("1121", "Koridor_1121_L1", 1, 0, true);
        gedung.addEdge("1122", "Koridor_1121_L1", 1, 0, true);
        gedung.addEdge("1108", "Koridor_1121_L1", 1, 0, true);
        gedung.addEdge("1108", "Jendela_1108_L1", 1, 0, true);
        gedung.addEdge("Koridor_1123_L1", "Koridor_1121_L1", 8, 0, true);
        gedung.addEdge("Koridor_1123_L1", "1107", 1, 0, true);
        gedung.addEdge("Jendela_1107_L1", "1107", 1, 0, true);
        gedung.addEdge("Koridor_1123_L1", "1123", 1, 0, true);

        // Lantai 2
        gedung.addNode("R.Admin", "room", 2);
        gedung.addNode("1208", "room", 2);
        gedung.addNode("1209", "room", 2);
        gedung.addNode("1210", "room", 2);
        gedung.addNode("1211", "room", 2);
        gedung.addNode("1212", "room", 2);
        gedung.addNode("1213", "room", 2);
        gedung.addNode("1214", "room", 2);
        gedung.addNode("1215", "room", 2);
        gedung.addNode("1216", "room", 2);
        gedung.addNode("R.SidangDekanat", "room", 2);
        gedung.addNode("1201", "room", 2);
        gedung.addNode("1202", "room", 2);
        gedung.addNode("1203", "room", 2);
        gedung.addNode("1121", "room", 2);
        gedung.addNode("Musala_L2", "room", 2);
        gedung.addNode("1219", "room", 2);
        gedung.addNode("Toilet_L2", "room", 2);
        gedung.addNode("Koridor_1208_L2", "corridor", 2);
        gedung.addNode("Koridor_1210_L2", "corridor", 2);
        gedung.addNode("Koridor_1211_L2", "corridor", 2);
        gedung.addNode("Koridor_Tengah_L2", "corridor", 2);
        gedung.addNode("Koridor_Admin_L2", "corridor", 2);
        gedung.addNode("Koridor_Belakang_L2", "corridor", 2);
        gedung.addNode("Koridor_Depan_L2", "corridor", 2);
        gedung.addNode("Koridor_1214_L2", "corridor", 2);
        gedung.addNode("Koridor_1215_L2", "corridor", 2);
        gedung.addNode("Koridor_1216_L2", "corridor", 2);
        gedung.addNode("Jendela_Selatan_L2", "window", 2);
        gedung.addNode("Jendela_Depan_L2", "window", 2);
        gedung.addNode("Jendela_Belakang_L2", "window", 2);
        gedung.addNode("Tangga_Depan_L2", "stairs", 2);
        gedung.addNode("Tangga_Belakang_L2", "stairs", 2);

        // Menambahkan Edges
        gedung.addEdge("Jendela_Selatan_L2", "Koridor_1208_L2", 2, 0, true);
        gedung.addEdge("1208", "Koridor_1208_L2", 1, 0, true);
        gedung.addEdge("Koridor_1210_L2", "Koridor_1208_L2", 4, 0, true);
        gedung.addEdge("Koridor_1210_L2", "1209", 1, 0, true);
        gedung.addEdge("Koridor_1210_L2", "1210", 1, 0, true);
        gedung.addEdge("Koridor_1210_L2", "Koridor_1211_L2", 4, 0, true);
        gedung.addEdge("1211", "Koridor_1211_L2", 1, 0, true);
        gedung.addEdge("Koridor_Tengah_L2", "Koridor_1211_L2", 6, 0, true);
        gedung.addEdge("Koridor_Tengah_L2", "1212", 1, 6, true);
        gedung.addEdge("Koridor_Belakang_L2", "1212", 1, 6, true);
        gedung.addEdge("Koridor_Belakang_L2", "Musala_L2", 1, 0, true);
        gedung.addEdge("Koridor_Belakang_L2", "Toilet_L2", 6, 0, true);
        gedung.addEdge("Koridor_Belakang_L2", "Tangga_Belakang_L2", 1, 0, true);
        gedung.addEdge("1219", "Tangga_Belakang_L2", 1, 0, true);
        gedung.addEdge("Jendela_Belakang_L2", "Tangga_Belakang_L2", 1, 0, true);
        gedung.addEdge("Koridor_Tengah_L2", "Koridor_Belakang_L2", 6, 0, true);
        gedung.addEdge("Koridor_Tengah_L2", "Koridor_Admin_L2", 8, 0, true);
        gedung.addEdge("Koridor_Depan_L2", "Koridor_Admin_L2", 2, 0, true);
        gedung.addEdge("Koridor_Depan_L2", "1121", 1, 0, true);
        gedung.addEdge("Koridor_Depan_L2", "Tangga_Depan_L2", 1, 0, true);
        gedung.addEdge("Koridor_Depan_L2", "Jendela_Depan_L2", 2, 0, true);
        gedung.addEdge("R.Admin", "Koridor_Admin_L2", 1, 22, true);
        gedung.addEdge("R.Admin", "Koridor_1208_L2", 1, 22, true);
        gedung.addEdge("Koridor_Tengah_L2", "Koridor_1214_L2", 1, 0, true);
        gedung.addEdge("1214", "Koridor_1214_L2", 1, 0, true);
        gedung.addEdge("Koridor_1215_L2", "Koridor_1214_L2", 4, 0, true);
        gedung.addEdge("Koridor_1215_L2", "1215", 1, 0, true);
        gedung.addEdge("Koridor_1215_L2", "1203", 1, 0, true);
        gedung.addEdge("Koridor_1215_L2", "Koridor_1216_L2", 4, 0, true);
        gedung.addEdge("1216", "Koridor_1216_L2", 1, 0, true);
        gedung.addEdge("1202", "Koridor_1216_L2", 1, 0, true);
        gedung.addEdge("1201", "Koridor_1216_L2", 1, 0, true);
        gedung.addEdge("R.SidangDekanat", "Koridor_1216_L2", 1, 0, true);

        // Lantai 3
        gedung.addNode("1301", "room", 3);
        gedung.addNode("1302", "room", 3);
        gedung.addNode("1303", "room", 3);
        gedung.addNode("1304", "room", 3);
        gedung.addNode("1306", "room", 3);
        gedung.addNode("1307", "room", 3);
        gedung.addNode("1308", "room", 3);
        gedung.addNode("1309", "room", 3);
        gedung.addNode("1310", "room", 3);
        gedung.addNode("1312", "room", 3);
        gedung.addNode("1313", "room", 3);
        gedung.addNode("1314", "room", 3);
        gedung.addNode("1315", "room", 3);
        gedung.addNode("1316", "room", 3);
        gedung.addNode("1320", "room", 3);
        gedung.addNode("Musala_L3", "room", 3);
        gedung.addNode("Toilet_L3", "room", 3);
        gedung.addNode("Koridor_1308_L3", "corridor", 3);
        gedung.addNode("Koridor_1310_L3", "corridor", 3);
        gedung.addNode("Koridor_1312_L3", "corridor", 3);
        gedung.addNode("Koridor_1313_L3", "corridor", 3);
        gedung.addNode("Koridor_1314_L3", "corridor", 3);
        gedung.addNode("Koridor_1315_L3", "corridor", 3);
        gedung.addNode("Koridor_Tengah_L3", "corridor", 3);
        gedung.addNode("Koridor_Depan_L3", "corridor", 3);
        gedung.addNode("Koridor_Belakang_L3", "corridor", 3);
        gedung.addNode("Jendela_Selatan_L3", "window", 3);
        gedung.addNode("Jendela_Utara_L3", "window", 3);
        gedung.addNode("Jendela_Depan_L3", "window", 3);
        gedung.addNode("Jendela_Belakang_L3", "window", 3);
        gedung.addNode("Tangga_Depan_L3", "stairs", 3);
        gedung.addNode("Tangga_Belakang_L3", "stairs", 3);

        // 2. Menambahkan Edges (IsBidirectional = true)
        gedung.addEdge("Jendela_Selatan_L3", "Koridor_1308_L3", 2, 0, true);
        gedung.addEdge("1308", "Koridor_1308_L3", 1, 0, true);
        gedung.addEdge("1307", "Koridor_1308_L3", 1, 0, true);
        gedung.addEdge("Koridor_1310_L3", "Koridor_1308_L3", 4, 0, true);
        gedung.addEdge("Koridor_1310_L3", "1309", 1, 0, true);
        gedung.addEdge("Koridor_1310_L3", "1310", 1, 0, true);
        gedung.addEdge("Koridor_1310_L3", "1306", 1, 0, true);
        gedung.addEdge("Koridor_1310_L3", "1304", 1, 5, true);
        gedung.addEdge("Koridor_1310_L3", "Koridor_1312_L3", 5, 0, true);
        gedung.addEdge("Koridor_1312_L3", "1304", 1, 5, true);
        gedung.addEdge("Koridor_1312_L3", "1312", 1, 4, true);
        gedung.addEdge("Koridor_Tengah_L3", "1312", 2, 4, true);
        gedung.addEdge("Koridor_Tengah_L3", "Koridor_Depan_L3", 10, 0, true);
        gedung.addEdge("1316", "Koridor_Depan_L3", 1, 0, true);
        gedung.addEdge("Tangga_Depan_L3", "Koridor_Depan_L3", 1, 0, true);
        gedung.addEdge("Jendela_Depan_L3", "Koridor_Depan_L3", 1, 0, true);
        gedung.addEdge("Koridor_Tengah_L3", "Koridor_1313_L3", 1, 0, true);
        gedung.addEdge("1313", "Koridor_1313_L3", 1, 0, true);
        gedung.addEdge("1303", "Koridor_1313_L3", 1, 0, true);
        gedung.addEdge("Koridor_1314_L3", "Koridor_1313_L3", 6, 0, true);
        gedung.addEdge("Koridor_1314_L3", "1314", 1, 0, true);
        gedung.addEdge("Koridor_1314_L3", "1302", 1, 0, true);
        gedung.addEdge("Koridor_1314_L3", "Koridor_1315_L3", 6, 0, true);
        gedung.addEdge("1315", "Koridor_1315_L3", 1, 0, true);
        gedung.addEdge("1301", "Koridor_1315_L3", 1, 0, true);
        gedung.addEdge("Jendela_Utara_L3", "Koridor_1315_L3", 6, 0, true);
        gedung.addEdge("Koridor_Tengah_L3", "Koridor_Belakang_L3", 6, 0, true);
        gedung.addEdge("Musala_L3", "Koridor_Belakang_L3", 1, 0, true);
        gedung.addEdge("Toilet_L3", "Koridor_Belakang_L3", 6, 0, true);
        gedung.addEdge("Tangga_Belakang_L3", "Koridor_Belakang_L3", 6, 0, true);
        gedung.addEdge("Tangga_Belakang_L3", "1320", 1, 0, true);
        gedung.addEdge("Tangga_Belakang_L3", "Jendela_Belakang_L3", 1, 0, true);
        
        // Lantai 4
        gedung.addNode("1401", "room", 4);
        gedung.addNode("1402", "room", 4);
        gedung.addNode("1403", "room", 4);
        gedung.addNode("1404", "room", 4);
        gedung.addNode("1405", "room", 4);
        gedung.addNode("1406", "room", 4);
        gedung.addNode("1407", "room", 4);
        gedung.addNode("1408", "room", 4);
        gedung.addNode("1409", "room", 4);
        gedung.addNode("1410", "room", 4);
        gedung.addNode("1411", "room", 4);
        gedung.addNode("1412", "room", 4);
        gedung.addNode("1413", "room", 4);
        gedung.addNode("1414", "room", 4);
        gedung.addNode("1415", "room", 4);
        gedung.addNode("1416", "room", 4);
        gedung.addNode("Musala_L4", "room", 4);
        gedung.addNode("Toilet_L4", "room", 4);
        gedung.addNode("Koridor_1407_L4", "corridor", 4);
        gedung.addNode("Koridor_1408_L4", "corridor", 4);
        gedung.addNode("Koridor_1409_L4", "corridor", 4);
        gedung.addNode("Koridor_1410_L4", "corridor", 4);
        gedung.addNode("Koridor_1411_L4", "corridor", 4);
        gedung.addNode("Koridor_1412_L4", "corridor", 4);
        gedung.addNode("Koridor_1413_L4", "corridor", 4);
        gedung.addNode("Jendela_Utara_L4", "window", 4);
        gedung.addNode("Jendela_Depan_L4", "window", 4);
        gedung.addNode("Jendela_Belakang_L4", "window", 4);
        gedung.addNode("Tangga_Depan_L4", "stairs", 4);
        gedung.addNode("Tangga_Belakang_L4", "stairs", 4);
        gedung.addNode("Tangga_Belakang_L4", "stairs", 4);

        // Menambahkan Edges
        gedung.addEdge("1416", "Koridor_1407_L4", 1, 0, true);
        gedung.addEdge("1407", "Koridor_1407_L4", 1, 0, true);
        gedung.addEdge("1406", "Koridor_1407_L4", 1, 0, true);
        gedung.addEdge("Koridor_1408_L4", "Koridor_1407_L4", 4, 0, true);
        gedung.addEdge("Koridor_1408_L4", "1408", 1, 0, true);
        gedung.addEdge("Koridor_1408_L4", "1405", 1, 8, true);
        gedung.addEdge("Koridor_1408_L4", "Koridor_Tengah_L4", 8, 0, true);
        gedung.addEdge("1405", "Koridor_Tengah_L4", 1, 8, true);
        gedung.addEdge("1409", "Koridor_Tengah_L4", 1, 0, true);
        gedung.addEdge("Koridor_Belakang_L4", "Koridor_Tengah_L4", 6, 0, true);
        gedung.addEdge("Koridor_Belakang_L4", "Musala_L4", 1, 0, true);
        gedung.addEdge("Koridor_Belakang_L4", "Toilet_L4", 6, 0, true);
        gedung.addEdge("Koridor_Belakang_L4", "Tangga_Belakang_L4", 1, 0, true);
        gedung.addEdge("1414", "Tangga_Belakang_L4", 1, 0, true);
        gedung.addEdge("Jendela_Belakang_L4", "Tangga_Belakang_L4", 1, 0, true);
        gedung.addEdge("Koridor_Depan_L4", "Koridor_Tengah_L4", 10, 0, true);
        gedung.addEdge("Koridor_Depan_L4", "1415", 1, 0, true);
        gedung.addEdge("Koridor_Depan_L4", "Tangga_Depan_L4", 1, 0, true);
        gedung.addEdge("Koridor_Depan_L4", "Jendela_Depan_L4", 1, 0, true);
        gedung.addEdge("Koridor_1410_L4", "Koridor_Tengah_L4", 10, 0, true);
        gedung.addEdge("Koridor_1410_L4", "1410", 1, 0, true);
        gedung.addEdge("Koridor_1410_L4", "1404", 1, 0, true);
        gedung.addEdge("Koridor_1410_L4", "Koridor_1411_L4", 4, 0, true);
        gedung.addEdge("1411", "Koridor_1411_L4", 1, 0, true);
        gedung.addEdge("1403", "Koridor_1411_L4", 1, 0, true);
        gedung.addEdge("Koridor_1412_L4", "Koridor_1411_L4", 4, 0, true);
        gedung.addEdge("Koridor_1412_L4", "1412", 1, 0, true);
        gedung.addEdge("Koridor_1412_L4", "1402", 1, 0, true);
        gedung.addEdge("Koridor_1412_L4", "Koridor_1413_L4", 4, 0, true);
        gedung.addEdge("1413", "Koridor_1413_L4", 1, 0, true);
        gedung.addEdge("1401", "Koridor_1413_L4", 1, 0, true);
        gedung.addEdge("Jendela_Utara_L4", "Koridor_1413_L4", 1, 0, true);

        // Lantai 5
        gedung.addNode("1501", "room", 5);
        gedung.addNode("1502", "room", 5);
        gedung.addNode("1503", "room", 5);
        gedung.addNode("1504", "room", 5);
        gedung.addNode("1505", "room", 5);
        gedung.addNode("1506", "room", 5);
        gedung.addNode("1507", "room", 5);
        gedung.addNode("1508", "room", 5);
        gedung.addNode("1509", "room", 5);
        gedung.addNode("1510", "room", 5);
        gedung.addNode("1511", "room", 5);
        gedung.addNode("1512", "room", 5);
        gedung.addNode("1513", "room", 5);
        gedung.addNode("1514", "room", 5);
        gedung.addNode("1515", "room", 5);
        gedung.addNode("1516", "room", 5);
        gedung.addNode("1517", "room", 5);
        gedung.addNode("Public_Space", "room", 5);
        gedung.addNode("Multimedia", "room", 5);
        gedung.addNode("Musala_L5", "room", 5);
        gedung.addNode("Toilet_L5", "room", 5);
        gedung.addNode("Koridor_1510_L5", "corridor", 5);
        gedung.addNode("Koridor_1509_L5", "corridor", 5);
        gedung.addNode("Koridor_1508_L5", "corridor", 5);
        gedung.addNode("Koridor_1507_L5", "corridor", 5);
        gedung.addNode("Koridor_1506_L5", "corridor", 5);
        gedung.addNode("Koridor_Depan_L5", "corridor", 5);
        gedung.addNode("Koridor_Tengah_L5", "corridor", 5);
        gedung.addNode("Koridor_Belakang_L5", "corridor", 5);
        gedung.addNode("Jendela_Selatan_L5", "window", 5);
        gedung.addNode("Jendela_Depan_L5", "window", 5);
        gedung.addNode("Jendela_Belakang_L5", "window", 5);
        gedung.addNode("Tangga_Depan_L5", "stairs", 5);
        gedung.addNode("Tangga_Belakang_L5", "stairs", 5);

        // 2. Menambahkan Edges (IsBidirectional = true)
        gedung.addEdge("Tangga_Belakang_L5", "Jendela_Belakang_L5", 1, 0, true);
        gedung.addEdge("Tangga_Belakang_L5", "1517", 1, 0, true);
        gedung.addEdge("Tangga_Belakang_L5", "Koridor_Belakang_L5", 1, 0, true);
        gedung.addEdge("Toilet_L5", "Koridor_Belakang_L5", 6, 0, true);
        gedung.addEdge("Musala_L5", "Koridor_Belakang_L5", 1, 0, true);
        gedung.addEdge("Koridor_Tengah_L5", "Koridor_Belakang_L5", 6, 0, true);
        gedung.addEdge("Koridor_Tengah_L5", "Koridor_1507_L5", 1, 0, true);
        gedung.addEdge("1507", "Koridor_1507_L5", 1, 0, true);
        gedung.addEdge("1505", "Koridor_1507_L5", 1, 0, true);
        gedung.addEdge("Koridor_1508_L5", "Koridor_1507_L5", 6, 0, true);
        gedung.addEdge("Koridor_1508_L5", "1508", 1, 0, true);
        gedung.addEdge("Koridor_1508_L5", "1504", 1, 0, true);
        gedung.addEdge("Koridor_1508_L5", "Koridor_1509_L5", 3, 0, true);
        gedung.addEdge("1509", "Koridor_1509_L5", 1, 0, true);
        gedung.addEdge("1503", "Koridor_1509_L5", 1, 0, true);
        gedung.addEdge("Koridor_1510_L5", "Koridor_1509_L5", 3, 0, true);
        gedung.addEdge("Koridor_1510_L5", "1510", 1, 0, true);
        gedung.addEdge("Koridor_1510_L5", "1502", 1, 0, true);
        gedung.addEdge("Koridor_1510_L5", "1501", 1, 0, true);
        gedung.addEdge("Koridor_1510_L5", "Jendela_Utara_L5", 1, 0, true);
        gedung.addEdge("Koridor_Tengah_L5", "Koridor_1506_L5", 2, 0, true);
        gedung.addEdge("Koridor_Depan_L5", "Koridor_1506_L5", 8, 0, true);
        gedung.addEdge("Koridor_Depan_L5", "1516", 1, 0, true);
        gedung.addEdge("Koridor_Depan_L5", "Tangga_Depan_L5", 1, 0, true);
        gedung.addEdge("Koridor_Depan_L5", "Jendela_Depan_L5", 1, 0, true);
        gedung.addEdge("1506", "Koridor_1506_L5", 1, 0, true);
        gedung.addEdge("Public_Space", "Koridor_1506_L5", 2, 0, true);
        gedung.addEdge("Public_Space", "Multimedia", 1, 0, true);
        gedung.addEdge("Public_Space", "1511", 1, 0, true);
        gedung.addEdge("Public_Space", "1512", 3, 0, true);
        gedung.addEdge("Public_Space", "1513", 3, 0, true);
        gedung.addEdge("Public_Space", "1514", 3, 0, true);
        gedung.addEdge("Public_Space", "1515", 1, 0, true);

        // Lantai 6
        gedung.addNode("Aula", "room", 6);
        gedung.addNode("1606", "room", 6);
        gedung.addNode("1605", "room", 6);
        gedung.addNode("Toilet_L6", "room", 6);
        gedung.addNode("Koridor_Depan_L6", "corridor", 6);
        gedung.addNode("Koridor_Tengah_L6", "corridor", 6);
        gedung.addNode("Koridor_Belakang_L6", "corridor", 6);
        gedung.addNode("Jendela_Depan_L6", "window", 6);
        gedung.addNode("Jendela_Belakang_L6", "window", 6);
        gedung.addNode("Tangga_Depan_L6", "stairs", 6);
        gedung.addNode("Tangga_Belakang_L6", "stairs", 6);

        // Menambahkan Edges
        gedung.addEdge("Koridor_Depan_L6", "Koridor_Tengah_L6", 8, 0, true);
        gedung.addEdge("Koridor_Tengah_L6", "Koridor_Belakang_L6", 8, 0, true);
        gedung.addEdge("Aula", "Koridor_Belakang_L6", 10, 8, true);
        gedung.addEdge("Aula", "Koridor_Depan_L6", 10, 8, true);
        gedung.addEdge("1606", "Koridor_Belakang_L6", 1, 0, true);
        gedung.addEdge("Toilet_L6", "Koridor_Belakang_L6", 1, 0, true);
        gedung.addEdge("Tangga_Belakang_L6", "Koridor_Belakang_L6", 1, 0, true);
        gedung.addEdge("Tangga_Belakang_L6", "Jendela_Belakang_L6", 1, 0, true);
        gedung.addEdge("1605", "Koridor_Depan_L6", 1, 0, true);
        gedung.addEdge("Tangga_Depan_L6", "Koridor_Depan_L6", 1, 0, true);
        gedung.addEdge("Jendela_Depan_L6", "Koridor_Depan_L6", 1, 0, true);

        // Menghubungkan lantai
        gedung.addEdge("Tangga_Depan_L1", "Tangga_Depan_L2", 6, 0, true);
        gedung.addEdge("Tangga_Depan_L3", "Tangga_Depan_L2", 6, 0, true);
        gedung.addEdge("Tangga_Depan_L3", "Tangga_Depan_L4", 6, 0, true);
        gedung.addEdge("Tangga_Depan_L5", "Tangga_Depan_L4", 6, 0, true);
        gedung.addEdge("Tangga_Depan_L5", "Tangga_Depan_L6", 6, 0, true);
        gedung.addEdge("Tangga_Belakang_L1", "Tangga_Belakang_L2", 6, 0, true);
        gedung.addEdge("Tangga_Belakang_L3", "Tangga_Belakang_L2", 6, 0, true);
        gedung.addEdge("Tangga_Belakang_L3", "Tangga_Belakang_L4", 6, 0, true);
        gedung.addEdge("Tangga_Belakang_L5", "Tangga_Belakang_L4", 6, 0, true);
        gedung.addEdge("Tangga_Belakang_L5", "Tangga_Belakang_L6", 6, 0, true);

        // 3. SIMULASI
        // Tentukan Posisi User
        String posisiUser = "1301";
        
        // Daftar Lokasi Bencana
        Set<String> daftarBencana = new HashSet<>();
        daftarBencana.add("koridor_1310_L3");

        
        // Cari jalan
        gedung.getEvacuationRoute(posisiUser, daftarBencana);
    }
}