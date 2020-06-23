package socketstockfish;

import spark.Request;
import spark.Response;
import spark.Route;
import xyz.niflheim.stockfish.StockfishClient;
import xyz.niflheim.stockfish.engine.enums.Query;
import xyz.niflheim.stockfish.engine.enums.QueryType;
import xyz.niflheim.stockfish.engine.enums.Variant;
import xyz.niflheim.stockfish.exceptions.StockfishInitException;
import java.util.function.Consumer;

public class AnalyseRequest implements Route, Consumer<String> {

    private final int ENGINE_TIMEOUT = 5000;

    private boolean waitForEngine = false;
    private long lastEngineRequest = 0;
    private String engineResponse = "";

    @Override
    public Object handle(Request request, Response response) throws Exception {
        String fen = request.params("fen");

        if (fen == null || fen.isEmpty()) {
            System.err.println("ERROR NOT_VALID_FEN, fen: " + fen);
            return "ERROR NOT_VALID_FEN";
        }

        System.out.println(fen);

        try {
            StockfishClient client = new StockfishClient.Builder()
                    .setInstances(4)
                    .setVariant(Variant.BMI2)
                    .build();
            Query query = new Query.Builder(QueryType.ANALYSE)
                    .setFen(fen)
//                    .setFen("rnbqkbnr/pppppppp/8/3P4/8/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1")
                    .setDepth(15)
                    .build();


            startEngineRequest();
            client.submit(query, this);

            while(waitForEngine && !this.hasTimeout()) {
                Thread.sleep(1);
            }

            if(this.hasTimeout()) {
                System.err.println("ERROR TIMEOUT_ENGINE, maybe invalid fen: " + fen);
                return "ERROR TIMEOUT_ENGINE";
            }
            System.out.println("OK: " + this.engineResponse);
            return this.engineResponse;

        } catch (StockfishInitException e) {
            System.err.println("UNKNOWN_ERROR: ");
            e.printStackTrace();
            return "ERROR UNKNOWN_ERROR";
        }
    }

    private void setEngineResult(String result) {
        this.engineResponse = result;
        this.waitForEngine = false;
    }

    void startEngineRequest() {
        waitForEngine = true;
        lastEngineRequest = System.currentTimeMillis();
    }

    boolean hasTimeout() {
        return (System.currentTimeMillis() - this.lastEngineRequest > ENGINE_TIMEOUT);
    }

    @Override
    public void accept(String result) {
        this.setEngineResult(result);
    }
}
