package mx.com.produban.itinterna.ksc.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public interface KibanaSourceService {

	public static final String TIPO_DASHBOARD = "dashboard";

	public static final String TIPO_VISUALIZATION = "visualization";

	public static final String TIPO_SEARCH = "search";
	
	public static final Logger log = Logger.getLogger(KibanaSourceService.class);

	List<String> obtenerIdObjetos(String host, Integer port, String index) throws MalformedURLException;

	void guardarObjetos(String host, Integer port, String index, String pathFile, String dashboard) throws MalformedURLException, JsonParseException, JsonMappingException, IOException;

	void despliegaObjetos(String host, Integer port, String index, String indexData, String pathFile) throws IOException;
}
