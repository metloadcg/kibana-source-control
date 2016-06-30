package mx.com.produban.itinterna.ksc.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class KibanaSourceServiceImpl implements KibanaSourceService {

	public List<String> obtenerIdObjetos(String host, Integer port, String index) throws MalformedURLException {

		RestTemplate restTemplate = new RestTemplate();

		String file = "/" + index + "/dashboard/_search";

		URL url = new URL("http", host, port, file);

		@SuppressWarnings("unchecked")
		Map<String, Object> mapaObjetos = restTemplate.getForObject(url.toString(), HashMap.class);

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> listaResult = (List<Map<String, Object>>) ((Map<String, Object>) mapaObjetos
				.get("hits")).get("hits");

		List<String> listResultIds = new ArrayList<String>(0);

		for (Map<String, Object> mapa : listaResult) {

			listResultIds.add(mapa.get("_id").toString());
		}

		return listResultIds;
	}

	public void guardarObjetos(String host, Integer port, String index, String pathFile, String dashboard)
			throws JsonParseException, JsonMappingException, IOException {

		List<Map<String, Object>> listaObjectos = new ArrayList<Map<String, Object>>(0);

		Map<String, Object> dashboardMap = obtenerDashboard(dashboard, host, port, index);

		listaObjectos.add(dashboardMap);

		List<Map<String, Object>> mapaVisualizaciones = obtenerVisualizaciones(dashboardMap, host, port, index);

		listaObjectos.addAll(mapaVisualizaciones);

		ObjectMapper mapper = new ObjectMapper();

		String jsonFile = mapper.writeValueAsString(listaObjectos);

		Path file = Paths.get(pathFile);

		CharSequence cs = jsonFile;

		List<CharSequence> iterable = new ArrayList<CharSequence>(0);

		iterable.add(cs);

		Files.createDirectories(file.getParent());

		Files.write(file, iterable, StandardCharsets.UTF_8, StandardOpenOption.CREATE);

		log.info("Se crea archivo :: " + pathFile);

	}

	@SuppressWarnings("unchecked")
	public void despliegaObjetos(String host, Integer port, String index, String indexData, String pathFile)
			throws IOException {

		Path file = Paths.get(pathFile);

		byte[] bytes = Files.readAllBytes(file);

		String json = new String(bytes, StandardCharsets.UTF_8);

		log.info("Se obtiene archivo :: " + pathFile);

		ObjectMapper mapper = new ObjectMapper();

		List<Map<String, Object>> listaObjetosKibana = mapper.readValue(json, List.class);

		RestTemplate restTemplate = new RestTemplate();

		log.info("Se ponen los siguientes objetos en este indice :: " + host + ":" + port + "/" + index);

		for (Map<String, Object> objeto : listaObjetosKibana) {

			String id = (String) objeto.get("_id");

			String type = (String) objeto.get("_type");

			Map<String, Object> mapJsonObjectSource = (Map<String, Object>) objeto.get("_source");

			if (!indexData.equals("")) {

				String searchSourceJson = (String) ((Map<String, Object>) ((Map<String, Object>) mapJsonObjectSource
						.get("kibanaSavedObjectMeta"))).get("searchSourceJSON");

				Map<String, Object> mapSearchSourceJSON = mapper.readValue(searchSourceJson, HashMap.class);

				if (mapSearchSourceJSON.containsKey("index")) {

					mapSearchSourceJSON.put("index", indexData);
				}

				searchSourceJson = mapper.writeValueAsString(mapSearchSourceJSON);

				((Map<String, Object>) ((Map<String, Object>) mapJsonObjectSource.get("kibanaSavedObjectMeta")))
						.put("searchSourceJSON", searchSourceJson);
			}

			String jsonObjectSource = mapper.writeValueAsString(mapJsonObjectSource);

			String path = "/" + index + "/" + type + "/" + id;

			URL urlVisualization;

			urlVisualization = new URL("http", host, port, path);

			HttpHeaders headers = new HttpHeaders();

			HttpEntity<?> entity = new HttpEntity<Object>(jsonObjectSource, headers);

			restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));

			Map<String, Object> result = restTemplate.postForObject(urlVisualization.toString(), entity, HashMap.class);

			log.info("ID : " + id + ", TYPE : " + type + ", RESULT : " + result.get("created"));

		}

	}

	private Map<String, Object> obtenerDashboard(String dashboard, String host, Integer port, String index)
			throws MalformedURLException, UnsupportedEncodingException {
		RestTemplate restTemplate = new RestTemplate();

		String dashboardEncode = URLEncoder.encode(dashboard, "UTF-8");

		String file = "/" + index + "/dashboard/" + dashboardEncode;

		URL url = new URL("http", host, port, file);

		@SuppressWarnings("unchecked")
		Map<String, Object> dashboardMap = restTemplate.getForObject(url.toString(), HashMap.class);

		log.info("Se obtiene dashboard : " + dashboard);

		return dashboardMap;
	}

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> obtenerVisualizaciones(Map<String, Object> dashboardMap, String host,
			Integer port, String index) throws JsonParseException, JsonMappingException, IOException {

		String jsonListaVisualizaciones = (String) ((Map<String, Object>) dashboardMap.get("_source"))
				.get("panelsJSON");

		RestTemplate restTemplate = new RestTemplate();

		ObjectMapper mapper = new ObjectMapper();
		
		List<Map<String, Object>> listaVisualizacionesId = mapper.readValue(jsonListaVisualizaciones, List.class);

		List<String> listaIdVisualizations = new ArrayList<String>(0);

		List<String> listaIdSearchs = new ArrayList<String>(0);

		log.info("Lista de visualizaciones del dashboard :: ");

		for (Map<String, Object> visualizacion : listaVisualizacionesId) {

			if (((String) visualizacion.get("type")).equals("search")) {

				listaIdSearchs.add((String) visualizacion.get("id"));

				log.info(visualizacion.get("id") + " ::: type : SEARCH");
			}
			if (((String) visualizacion.get("type")).equals("visualization")) {

				listaIdVisualizations.add((String) visualizacion.get("id"));

				log.info(visualizacion.get("id") + " ::: type : VISUALIZATION");
			}
		}

		String jsonIdVisualizations = mapper.writeValueAsString(listaIdVisualizations);
		
		String jsonQueryVisualizations = "{\"from\": 0,\"size\": 1000,\"query\": {\"filtered\": {\"filter\": {\"ids\": {\"values\":"
				+ jsonIdVisualizations + "}}}}}";
		
		String path = "/" + index + "/visualization/_search";

		URL urlVisualization = new URL("http", host, port, path);

		HttpEntity<?> entity = new HttpEntity<Object>(jsonQueryVisualizations);

		restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));

		
		Map<String, Object> mapaVisualizaciones = restTemplate.postForObject(urlVisualization.toString(), entity,
				HashMap.class);
		
		List<Map<String, Object>> listaResult = (List<Map<String, Object>>) ((Map<String, Object>) mapaVisualizaciones
				.get("hits")).get("hits");
		
		String jsonIdSearchs = mapper.writeValueAsString(listaIdSearchs);

		String jsonQuerySearchs = "{\"from\": 0,\"size\": 1000,\"query\": {\"filtered\": {\"filter\": {\"ids\": {\"values\":"
				+ jsonIdSearchs + "}}}}}";
		
		String pathSearch = "/" + index + "/search/_search";

		URL urlSearch = new URL("http", host, port, pathSearch);

		HttpEntity<?> entitySearch = new HttpEntity<Object>(jsonQuerySearchs);

		restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));

		
		Map<String, Object> mapaSearchs = restTemplate.postForObject(urlSearch.toString(), entitySearch,
				HashMap.class);
		
		listaResult.addAll((List<Map<String, Object>>) ((Map<String, Object>) mapaSearchs
				.get("hits")).get("hits"));
		
		log.info("Se obtienen visualizaciones correctamente");

		return listaResult;

	}

}
