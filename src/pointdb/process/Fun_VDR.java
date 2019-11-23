package pointdb.process;

@Tag("forest_structure")
@Description("[normalized] vertical distribution ratio, (ch - median) / ch,  based on BE_H_MAX (ch) and BE_H_MEDIAN (median)")
public class Fun_VDR extends ProcessingFun {
	@Override
	public double process(DataProvider2 provider) {		
		double ch = Fun_BE.Fun_BE_H_MAX.DEFAULT.process(provider);
		double median = Fun_BE.Fun_BE_H_MEDIAN.DEFAULT.process(provider);
		double vdr = (ch - median) / ch;
		return vdr;
	}
}