package pointcloud;

public class AttributeSelector {

	public boolean x;
	public boolean y;
	public boolean z;
	public boolean intensity;
	public boolean returnNumber;
	public boolean returns;
	public boolean scanDirectionFlag;
	public boolean edgeOfFlightLine;
	public boolean classification;
	public boolean scanAngleRank;
	public boolean gpsTime;
	public boolean red;
	public boolean green;
	public boolean blue;

	public static AttributeSelector of(String[] attributes) {
		AttributeSelector selector = new AttributeSelector();
		for (String attribute : attributes) {
			if(!attribute.isEmpty()) {
				selector.set(attribute);
			}
		}
		return selector;
	}

	public void set(String attribute) {
		set(attribute, true);
	}
	
	public void set(String attribute, boolean b) {
		switch(attribute) {
		case "x":
			x = b;
			break;
		case "y":
			y = b;
			break;
		case "z":
			z = b;
			break;
		case "intensity":
			intensity = b;
			break;
		case "returnNumber":
			returnNumber = b;
			break;
		case "returns":
			returns = b;
			break;
		case "scanDirectionFlag":
			scanDirectionFlag = b;
			break;
		case "edgeOfFlightLine":
			edgeOfFlightLine = b;
			break;
		case "classification":
			classification = b;
			break;
		case "scanAngleRank":
			scanAngleRank = b;
			break;
		case "gpsTime":
			gpsTime = b;
			break;
		case "red":
			red = b;
			break;
		case "green":
			green = b;
			break;
		case "blue":
			blue = b;
			break;
		default:
			throw new RuntimeException("unknown attribute: "+attribute);
		}
	}

	public AttributeSelector() {}

	public AttributeSelector(boolean b) {
		all(b);
	}

	public AttributeSelector all(boolean b) {
		x = b;
		y = b;
		z = b;
		intensity = b;
		returnNumber = b;
		returns = b;
		scanDirectionFlag = b;
		edgeOfFlightLine = b;
		classification = b;
		scanAngleRank = b;
		gpsTime = b;
		red = b;
		green = b;
		blue = b;
		return this;
	}

	public AttributeSelector all() {
		return all(true);
	}

	public AttributeSelector none() {
		return all(false);
	}

	public boolean hasXY() {
		return x && y;
	}

	public AttributeSelector copy() {
		AttributeSelector a = new AttributeSelector();
		a.x = x;
		a.y = y;
		a.z = z;
		a.intensity = intensity;
		a.returnNumber = returnNumber;
		a.returns = returns;
		a.scanDirectionFlag = scanDirectionFlag;
		a.edgeOfFlightLine = edgeOfFlightLine;
		a.classification = classification;
		a.scanAngleRank = scanAngleRank;
		a.gpsTime = gpsTime;
		a.red = red;
		a.green = green;
		a.blue = blue;
		return a;
	}

	public AttributeSelector setXY() {
		x = true;
		y = true;
		return this;
	}

	public AttributeSelector setXYZ() {
		x = true;
		y = true;
		z = true;
		return this;
	}
	
	public AttributeSelector setXYZI() {
		x = true;
		y = true;
		z = true;
		intensity = true;
		return this;
	}
	
	public AttributeSelector setRed() {
		red = true;
		return this;
	}
	
	public AttributeSelector setGreen() {
		green = true;
		return this;
	}
	
	public AttributeSelector setBlue() {
		blue = true;
		return this;
	}

	public AttributeSelector setClassification() {
		classification = true;
		return this;
	}

	public AttributeSelector setReturn() {
		this.returnNumber = true;
		this.returns = true;
		return this;
	}
	
	public AttributeSelector setReturnNumber() {
		this.returnNumber = true;
		return this;
	}
	
	public AttributeSelector setReturns() {
		this.returns = true;
		return this;
	}
	
	public AttributeSelector setScanAngleRank() {
		this.scanAngleRank = true;
		return this;
	}
	
	public int count() {
		int cnt = 0;		
		cnt += x ? 1 : 0;
		cnt += y ? 1 : 0;
		cnt += z ? 1 : 0;
		cnt += intensity ? 1 : 0;
		cnt += returnNumber ? 1 : 0;
		cnt += returns ? 1 : 0;
		cnt += scanDirectionFlag ? 1 : 0;
		cnt += edgeOfFlightLine ? 1 : 0;
		cnt += classification ? 1 : 0;
		cnt += scanAngleRank ? 1 : 0;
		cnt += gpsTime ? 1 : 0;
		cnt += red ? 1 : 0;
		cnt += green ? 1 : 0;
		cnt += blue ? 1 : 0;
		return cnt;
	}
	
	public String[] toArray() {
		String[] a = new String[count()];
		int pos = 0;
		if(x) {
			a[pos++] = "x";
		}
		if(y) {
			a[pos++] = "y";
		}
		if(z) {
			a[pos++] = "z";
		}
		if(intensity) {
			a[pos++] = "intensity";
		}
		if(returnNumber) {
			a[pos++] = "returnNumber";
		}
		if(returns) {
			a[pos++] = "returns";
		}
		if(scanDirectionFlag) {
			a[pos++] = "scanDirectionFlag";
		}
		if(edgeOfFlightLine) {
			a[pos++] = "edgeOfFlightLine";
		}
		if(classification) {
			a[pos++] = "classification";
		}
		if(scanAngleRank) {
			a[pos++] = "scanAngleRank";
		}
		if(gpsTime) {
			a[pos++] = "gpsTime";
		}
		if(red) {
			a[pos++] = "red";
		}
		if(green) {
			a[pos++] = "green";
		}
		if(blue) {
			a[pos++] = "blue";
		}
		return a;
	}
}
