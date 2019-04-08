package rasterdb.node;

import com.mkobos.pca_transform.PCA;

import Jama.Matrix;
import rasterdb.BandProcessor;
import util.frame.DoubleFrame;

public class ProcessorNode_pca extends ProcessorNode {

	private final ProcessorNode targetNode;
	private final int maxComponents;

	public ProcessorNode_pca(ProcessorNode targetNode, int maxComponents) {
		this.targetNode = targetNode;
		this.maxComponents = maxComponents;
	}

	@Override
	public DoubleFrame[] process(BandProcessor processor) {
		DoubleFrame[] targetFrames = targetNode.process(processor);
		Matrix matrix = new Matrix(toMatrix(targetFrames));
		PCA pca = new PCA(matrix);
		Matrix transformedMatrix = pca.transform(matrix, PCA.TransformationType.ROTATION);
		int components = Math.min(transformedMatrix.getColumnDimension(), maxComponents);
		DoubleFrame[] resultFrames = toDoubleFrames(transformedMatrix.getArray(), targetFrames[0], components);
		for (int i = 0; i < resultFrames.length; i++) {
			resultFrames[i].meta.put("name", "pca"+(i+1));
		}
		return resultFrames;
	}

	public static double[][] toMatrix(DoubleFrame[] targetFrames) {
		int dims = targetFrames.length;
		int w = targetFrames[0].width;
		int h = targetFrames[0].height;
		int len =  w * h; 
		double m[][] = new double[len][dims];
		double[][][] datas = new double[dims][][];
		for(int d=0; d<dims; d++) {
			datas[d] = targetFrames[d].data;
		}
		int pos = 0;
		for(int y=0; y<h; y++) {
			for(int x=0; x<w; x++) {
				double[] mrow = m[pos++];
				for(int d=0; d<dims; d++) {
					mrow[d] = datas[d][y][x];
				}
			}
		}
		return m;
	}
	
	public static DoubleFrame[] toDoubleFrames(double[][] matrix, DoubleFrame ref, int components) {
		//int dims = matrix[0].length;
		int dims = components;
		DoubleFrame[] targetFrames = new DoubleFrame[dims];
		double[][][] datas = new double[dims][][];
		for(int d=0; d<dims; d++) {
			targetFrames[d] = DoubleFrame.ofExtent(ref);
			datas[d] = targetFrames[d].data;
		}
		int w = targetFrames[0].width;
		int h = targetFrames[0].height;
		int pos = 0;
		for(int y=0; y<h; y++) {
			for(int x=0; x<w; x++) {
				double[] mrow = matrix[pos++];
				for(int d=0; d<dims; d++) {
					datas[d][y][x] = mrow[d];
				}
			}
		}
		return targetFrames;
	}

}
