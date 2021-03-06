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
import java.util.function.Consumer;

public class AnalyseRequest implements Route, Consumer<String> {

    private final int ENGINE_TIMEOUT = 5000;

    private boolean waitForEngine = false;
    private long lastEngineRequest = 0;
    private String engineResponse = "";

    @Override
    public Object handle(Request request, Response response) throws Exception {
        response.type("application/json");
        String fen = request.params("fen");
        int difficulty = request.params("difficulty")== null ? -1 : Integer.parseInt(request.params("difficulty"));

        if (fen == null || fen.isEmpty()) {
            System.err.println("ERROR EMPTY_FEN, fen: " + fen);
            return this.buildError("EMPTY_FEN");
        }


        if(!fen.matches("\\s*([rnbqkpRNBQKP1-8]+\\/){7}([rnbqkpRNBQKP1-8]+)\\s[bw-]\\s(([a-hkqA-HKQ]{1,4})|(-))\\s(([a-h][36])|(-))\\s\\d+\\s\\d+\\s*")) {
            System.err.println("ERROR NOT_VALID_FEN, fen: " + fen);
            return this.buildError("NOT_VALID_FEN");
        }

        try {
            SimpleEngine engine = new SimpleEngine();

//            StockfishClient client = new StockfishClient.Builder()
//                    .setInstances(1)
//                    .setVariant(Variant.BMI2)
//                    .build();
            Query.Builder builder = new Query.Builder(QueryType.ANALYSE)
                    .setFen(fen)
                    .setDepth(15);
                // rnbqkbnr%2Fpppppppp%2F8%2F3P4%2F8%2F8%2FPPPP1PPP%2FRNBQKBNR%20b%20KQkq%20e3%200%201
            if(difficulty != -1) {
                builder.setDifficulty(difficulty);
            }

            Query query = builder.build();
            this.startEngineRequest();
            engine.requestEngine(query);

            String result = null;
            while (result == null && !this.hasTimeout()) {
                result = engine.getRawResponse();
                Thread.sleep(0, 10000);
            }


            if(this.hasTimeout() || result == null) {
                System.err.println("ERROR TIMEOUT_ENGINE, maybe invalid fen: " + fen);
                return this.buildError("TIMEOUT_ENGINE");
            }

            String jsonResponse = this.parseEngineResponse(result);

            System.out.println("OK: " + jsonResponse);
            return jsonResponse;

        } catch (StockfishInitException e) {
            System.err.println("UNKNOWN_ERROR: ");
            e.printStackTrace();
            return this.buildError("UNKNOWN_ERROR");
        }
    }

    private String buildError(String code) {
        JsonObject response = new JsonObject();
        response.addProperty("error", code);

        return response.toString();
    }

    private String parseEngineResponse(String engineResponse) {
        String[] splitted = engineResponse.split(" ");
        JsonObject response = new JsonObject();

        for(int i = 0; i < Math.floor(splitted.length/2.0)*2; i+= 2) {
            try{
                response.addProperty(splitted[i], Integer.parseInt(splitted[i+1]));
            } catch(Exception e) {
                response.addProperty(splitted[i], splitted[i+1]);
            }

        }

        return response.toString();
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
