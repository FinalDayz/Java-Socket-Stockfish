package socketstockfish;

import com.google.gson.JsonObject;
import spark.Request;
import spark.Response;
import spark.Route;
import xyz.niflheim.stockfish.StockfishClient;
import xyz.niflheim.stockfish.engine.enums.Query;
import xyz.niflheim.stockfish.engine.enums.QueryType;
import xyz.niflheim.stockfish.engine.enums.Variant;
import xyz.niflheim.stockfish.exceptions.StockfishInitException;

import java.util.ArrayList;
import java.util.function.Consumer;

public class Puzzle implements Route {

    private boolean waitForEngine = false;
    private String engineResponse = "";
    SimpleEngine engine;
    private JsonObject lastAnalisis;

    static String savedPuzzle = null;

    static String getPuzzle() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                savedPuzzle = null;
                savedPuzzle = (new Puzzle()).calcPuzzle();
            }
        }).start();
        while(savedPuzzle == null) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return savedPuzzle;
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        response.type("application/json");
        String puzzle = Puzzle.getPuzzle();


        System.out.println("{\"puzzle\":\"" + puzzle + "\"}");

        return "{\"puzzle\":\""+puzzle+"\"}";
    }

    public String calcPuzzle() {

        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

        this.engine = new SimpleEngine();

        try {
            int depth = 2;
            boolean goodMove = true;
            String prefFen = "";
            for (int i = 0; i < 100; i++) {
                goodMove = !goodMove;
                if (depth < 8 && i % 3 == 0)
                    depth++;

                fen = this.makeBestMove(fen,
                        1 + (goodMove ? 0 : 12),
                        depth
                );

                System.out.println(lastAnalisis);

                if (lastAnalisis.has("mate")) {
                    int mate = lastAnalisis.get("mate").getAsInt();

                    if (mate < 0 && Math.abs(mate) <= 5) {
                        mate = Math.abs(mate);
                        String mateFen = prefFen;
                        for (int j = mate; j > 3; j--)
                            mateFen = this.makeBestMove(prefFen, 20, 10);
                        mateFen = this.makeBestMove(prefFen, 20, 10);

                        return mateFen;
                    }
                }

                prefFen = fen;
            }

        } catch (Exception e) {

        }

        return this.calcPuzzle();

    }

    private ArrayList<String> getFollowingMoves(String fen) throws InterruptedException, StockfishInitException {
        ArrayList<String> moves = new ArrayList<String>();


        return moves;
    }

    private String makeBestMove(String fen, int difficulty, int depth) throws StockfishInitException, InterruptedException {
        Query query = new Query.Builder(QueryType.ANALYSE)
                .setFen(fen)
                .setDifficulty(difficulty)
                .setDepth(depth)
                .build();

        engine.requestEngine(query);
        JsonObject analisis = waitForJsonResult(engine);
        this.lastAnalisis = analisis;
        String bestMove = analisis.get("bestMove").getAsString();

        query = (new Query.Builder(QueryType.Make_Move))
                .setFen(fen)
                .setMove(bestMove)
                .build();

        engine.requestEngine(query);

        return waitForResult(engine);
    }

    private String waitForResult(SimpleEngine engine) throws InterruptedException {
        String result = null;
        while (result == null) {
            result = engine.getRawResponse();
            Thread.sleep(0, 10000);
        }
        return result;
    }

    private JsonObject waitForJsonResult(SimpleEngine engine) throws InterruptedException {
        JsonObject result = null;
        while (result == null) {
            result = engine.getResponse();
            Thread.sleep(0, 10000);
        }
        return result;
    }

    private String buildError(String code) {
        JsonObject response = new JsonObject();
        response.addProperty("error", code);

        return response.toString();
    }


}
