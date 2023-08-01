package broker;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;


import org.tinylog.Logger;
import org.yaml.snakeyaml.Yaml;

import broker.group.ExternalGroupConfig;
import pointdb.base.PointdbConfig;
import postgis.PostgisConfig;
import util.Util;
import util.yaml.YamlMap;

public class BrokerConfigYaml extends BrokerConfig {	

	public BrokerConfigYaml(String filename) {
		try {
			InputStream in = new FileInputStream(new File(filename));
			YamlMap configMap = YamlMap.ofObject(new Yaml().load(in));

			for(YamlMap pc:configMap.optList("pointdb").asMaps()) {
				PointdbConfig pointdbConfig = PointdbConfig.ofYAML(pc);
				if(Util.isValidID(pointdbConfig.name)) {
					pointdbMap.put(pointdbConfig.name, pointdbConfig);
				} else {
					Logger.warn("pointdb not inserted: invalid identifier: "+pointdbConfig.name+" in "+filename);
				}
			}

			for(YamlMap pc:configMap.optList("Points_of_interest").asMaps()) {
				ExternalGroupConfig poiConfig = ExternalGroupConfig.ofYAML(pc);
				if(Util.isValidID(poiConfig.name)) {
					poiGroupMap.put(poiConfig.name, poiConfig);
				} else {
					Logger.warn("poiGroup not inserted: invalid identifier: "+poiConfig.name+" in "+filename);
				}
			}

			for(YamlMap pc:configMap.optList("Regions_of_interest").asMaps()) {
				ExternalGroupConfig roiConfig = ExternalGroupConfig.ofYAML(pc);
				if(Util.isValidID(roiConfig.name)) {
					roiGroupMap.put(roiConfig.name, roiConfig);
				} else {
					Logger.warn("roiGroup not inserted: invalid identifier: "+roiConfig.name+" in "+filename);
				}
			}

			try {
				for(YamlMap yamlMap:configMap.optList("jws").asMaps()) {
					try {
						JwsConfig jwsConfig = JwsConfig.ofYAML(yamlMap);
						jwsConfigs.add(jwsConfig);
					} catch (Exception e) {
						Logger.warn(e);
					}
				}
			} catch (Exception e) {
				Logger.warn(e);
			}

			this.serverConfig = configMap.funMap("server", m -> ServerConfig.ofYAML(m), ServerConfig::new);
			this.postgisConfig = configMap.funMap("postgis", m -> PostgisConfig.ofYAML(m), PostgisConfig::new);
		}catch (Exception e) {
			e.printStackTrace();
			Logger.error("config YAML file error in "+filename+"  "+e);
			//throw new RuntimeException(e);
		}
	}
}
