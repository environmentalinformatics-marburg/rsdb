package util;

public class FillerBordered {
	
	public static boolean fillGBorderedSquare(float[][] src, float[][] dst, int width, int height, int fx, int fy, int radius) {
		int xmin = fx - radius;
		int ymin = fy - radius;
		int xmax = fx + radius;
		int ymax = fy + radius;

		float cnt = 0;
		float sum = 0;

		{
			float[] row = src[ymin];
			for (int ix = xmin; ix <= xmax; ix++) {
				float v = row[ix];
				if(Float.isFinite(v)) {
					sum += v;
					cnt++;
				}
			}
		}

		{
			float[] row = src[ymax];
			for (int ix = xmin; ix <= xmax; ix++) {
				float v = row[ix];
				if(Float.isFinite(v)) {
					sum += v;
					cnt++;
				}
			}
		}

		{
			for (int iy = ymin + 1; iy < ymax; iy++) {
				float v = src[iy][xmin];
				if(Float.isFinite(v)) {
					sum += v;
					cnt++;
				}
			}
		}

		{
			for (int iy = ymin + 1; iy < ymax; iy++) {
				float v = src[iy][xmax];
				if(Float.isFinite(v)) {
					sum += v;
					cnt++;
				}
			}
		}

		if(cnt > 0) {
			//Logger.info("fill "+fx+" "+fy+" "+cnt);
			dst[fy][fx] = sum / cnt;
			return true;
		}
		return false;
	}

	private static boolean fillGBorderedCircle(float[][] src, float[][] dst, int width, int height, int fx, int fy, int radius) { // based on https://de.wikipedia.org/wiki/Bresenham-Algorithmus
		float cnt = 0;
		float sum = 0;
		int f = 1 - radius;
		int ddF_x = 0;
		int ddF_y = -2 * radius;
		int x = 0;
		int y = radius;

		{
			float v = src[fy + radius][fx];
			if(Float.isFinite(v)) {
				sum += v;
				cnt++;
			}
		}

		{
			float v = src[fy - radius][fx];
			if(Float.isFinite(v)) {
				sum += v;
				cnt++;
			}
		}

		{
			float v = src[fy][fx + radius];
			if(Float.isFinite(v)) {
				sum += v;
				cnt++;
			}
		}

		{
			float v = src[fy][fx - radius];
			if(Float.isFinite(v)) {
				sum += v;
				cnt++;
			}
		}


		while(x < y) {
			if (f >= 0) {
				y -= 1;
				ddF_y += 2;
				f += ddF_y;
			}
			x += 1;
			ddF_x += 2;
			f += ddF_x + 1;

			{
				float v = src[fy + y][fx + x];
				if(Float.isFinite(v)) {
					sum += v;
					cnt++;
				}
			}

			{
				float v = src[fy + y][fx - x];
				if(Float.isFinite(v)) {
					sum += v;
					cnt++;
				}
			}

			{
				float v = src[fy - y][fx + x];
				if(Float.isFinite(v)) {
					sum += v;
					cnt++;
				}
			}

			{
				float v = src[fy - y][fx - x];
				if(Float.isFinite(v)) {
					sum += v;
					cnt++;
				}
			}

			{
				float v = src[fy + x][fx + y];
				if(Float.isFinite(v)) {
					sum += v;
					cnt++;
				}
			}

			{
				float v = src[fy + x][fx - y];
				if(Float.isFinite(v)) {
					sum += v;
					cnt++;
				}
			}

			{
				float v = src[fy - x][fx + y];
				if(Float.isFinite(v)) {
					sum += v;
					cnt++;
				}
			}

			{
				float v = src[fy - x][fx - y];
				if(Float.isFinite(v)) {
					sum += v;
					cnt++;
				}
			}

		}

		if(cnt > 0) {
			//Logger.info("fill "+fx+" "+fy+" "+cnt);
			dst[fy][fx] = sum / cnt;
			return true;
		}
		return false;
	}
}
