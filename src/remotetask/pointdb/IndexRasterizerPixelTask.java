package remotetask.pointdb;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pointdb.base.Rect;
import pointdb.process.DataProvider2;
import pointdb.process.ProcessingFun;
import util.BlokingTaskSubmitter;
import util.BlokingTaskSubmitter.PhasedTask;

public class IndexRasterizerPixelTask extends PhasedTask {
	private static final Logger log = LogManager.getLogger();
	private static final long serialVersionUID = -7212334543686038134L;
	
	private final DataProvider2Factory dpFactory;
	private final Rect pRect;
	private final ProcessingFun[] indices;
	private final float[][][] pixels;
	private final int x;
	private final int y;

	public IndexRasterizerPixelTask(BlokingTaskSubmitter blokingTaskSubmitter, DataProvider2Factory dpFactory, Rect pRect, ProcessingFun[] indices, float[][][] pixels, int x, int y) {
		super(blokingTaskSubmitter);
		this.dpFactory = dpFactory;
		this.pRect = pRect;
		this.indices = indices;
		this.pixels = pixels;
		this.x = x;
		this.y = y;
	}

	@Override
	public void run() {
		DataProvider2 dp = dpFactory.get(pRect);
		int indices_len = indices.length;
		for (int i = 0; i < indices_len; i++) {
			try {						
				pixels[i][y][x] = (float) indices[i].process(dp);
			} catch(Exception e) {
				//e.printStackTrace();					
				log.warn(e);
				pixels[i][y][x] = Float.NaN;
			}
		}
		dp.close();			
	}		
}