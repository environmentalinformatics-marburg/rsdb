package remotetask.rasterdb;

public class BandSpec {
	public String band_name = null;
	public int file_band_index = -1;
	public int rasterdb_band_index = -1;
	public int rastedb_band_data_type = -1;
	public int gdal_raster_data_type = -1;
	public double wavelength = Double.NaN;
	public double fwhm = Double.NaN;
	public String visualisation = null;
	public Double no_data_value = null;
	public boolean import_band = true;
	public int timestamp = -1;
	public String timeSlice = null;
	public double value_scale = Double.NaN;
	public double value_offset = Double.NaN;
	@Override
	public String toString() {
		return "BandSpec [band_name=" + band_name + ", file_band_index=" + file_band_index + ", rasterdb_band_index="
				+ rasterdb_band_index + ", rastedb_band_data_type=" + rastedb_band_data_type
				+ ", gdal_raster_data_type=" + gdal_raster_data_type + ", wavelength=" + wavelength + ", fwhm=" + fwhm
				+ ", visualisation=" + visualisation + ", no_data_value=" + no_data_value + ", import_band="
				+ import_band + ", timestamp=" + timestamp + "]";
	}
}