package mx.com.produban.itinterna.ksc.main;

import java.io.IOException;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import mx.com.produban.itinterna.ksc.service.KibanaSourceService;
import mx.com.produban.itinterna.ksc.service.KibanaSourceServiceImpl;

public class KibanaSourceControlMain {

	private static final String PARAM_PATH_FILE = "pathFile";

	private static final String PARAM_INDEX = "index";

	private static final String PARAM_PORT = "port";

	private static final String PARAM_HOST = "host";

	private static final String PARAM_DASHBOARD_ID = "dashboardId";

	private static final String PARAM_INDEX_DATA = "indexData";

	private static final String PARAM_PUT = "PUT";

	private static final String PARAM_GET = "GET";

	private static final String PARAM_LIST = "LIST";

	private static final Logger log = Logger.getLogger(KibanaSourceControlMain.class);

	private static CommandLine cmd;

	private static KibanaSourceService kibanaSourceService = new KibanaSourceServiceImpl();

	private static Options options;

	private static String host;

	private static Integer port;

	private static String index;

	public static void main(String args[]) throws JsonParseException, JsonMappingException, IOException {

		options = createOptions();

		validateGenericArgs(args);

		try {

			if (cmd.hasOption(PARAM_LIST)) {

				log.info("Lista de Dashboards :: ");

				List<String> listaIds = kibanaSourceService.obtenerIdObjetos(host, port, index);

				for (String id : listaIds) {

					log.info(id);
				}

			} else if (cmd.hasOption(PARAM_GET)) {

				String dashboard;

				if (cmd.hasOption(PARAM_DASHBOARD_ID)) {

					dashboard = cmd.getOptionValue(PARAM_DASHBOARD_ID);

				} else {

					throw new ParseException("Parametro dashboard requerido");

				}

				String pathFile;

				if (cmd.hasOption(PARAM_PATH_FILE)) {

					pathFile = cmd.getOptionValue(PARAM_PATH_FILE);

				} else {

					throw new ParseException("Parametro pathFile requerido");

				}

				kibanaSourceService.guardarObjetos(host, port, index, pathFile, dashboard);

			} else if (cmd.hasOption(PARAM_PUT)) {

				String pathFile;

				if (cmd.hasOption(PARAM_PATH_FILE)) {

					pathFile = cmd.getOptionValue(PARAM_PATH_FILE);

				} else {

					throw new ParseException("Parametro pathFile requerido");

				}

				String indexData;

				if (cmd.hasOption(PARAM_INDEX_DATA)) {

					indexData = cmd.getOptionValue(PARAM_INDEX_DATA);

				} else {

					indexData = "";

				}

				kibanaSourceService.despliegaObjetos(host, port, index, indexData, pathFile);

			}

		} catch (ParseException parseException) {

			HelpFormatter formatter = new HelpFormatter();

			formatter.printHelp("Kibana-Source-Control", options);

			log.error(parseException.getMessage(), parseException);
		}

	}

	private static void validateGenericArgs(String... args) {

		CommandLineParser parser = new DefaultParser();

		try {

			cmd = parser.parse(options, args);

			boolean list = cmd.hasOption(PARAM_LIST);

			boolean get = cmd.hasOption(PARAM_GET);

			boolean put = cmd.hasOption(PARAM_PUT);

			if ((list && get) || (list && put) || (get && put)) {

				throw new ParseException("Solo se permite una operación LIST, GET o PUSH");
			}

			if (cmd.hasOption(PARAM_HOST)) {

				host = cmd.getOptionValue(PARAM_HOST);

			} else {

				throw new ParseException("Parametro Host requerido");
			}

			if (cmd.hasOption(PARAM_PORT)) {

				port = Integer.parseInt(cmd.getOptionValue(PARAM_PORT));

			} else {

				throw new ParseException("Parametro Port requerido");
			}

			if (cmd.hasOption(PARAM_INDEX)) {

				index = cmd.getOptionValue(PARAM_INDEX);

			} else {

				throw new ParseException("Parametro Index requerido");
			}

		} catch (ParseException parseException) {

			HelpFormatter formatter = new HelpFormatter();

			formatter.printHelp("Kibana-Source-Control", options);

			log.error(parseException.getMessage(), parseException);

		}
	}

	private static Options createOptions() {

		Options options = new Options();

		options.addOption(PARAM_LIST, false, "Operación para listar los id de los dashboards en un indice");
		options.addOption(PARAM_GET, false,
				"Operación para obtener un dashboard con sus visualizaciones y guardarlo en un archivo");
		options.addOption(PARAM_PUT, false,
				"Operación para poner un dashboard con sus visualizaciones en un indice determinado desde un archivo");
		options.addOption(PARAM_DASHBOARD_ID, true, "Id del dashboard dentro del indice a guardar");
		options.addOption(PARAM_HOST, true, "Host donde se encuentra elasticsearch");
		options.addOption(PARAM_PORT, true, "Puerto donde se encuentra elasticsearch");
		options.addOption(PARAM_INDEX, true, "Nombre de indice de kibana (por default .kibana)");
		options.addOption(PARAM_INDEX_DATA, true,
				"Nombre del indice donde las visualizaciones obtienen los datos, si el parametro no viene se toma el mismo indice de donde se exportaron las visualizaciones");
		options.addOption(PARAM_PATH_FILE, true,
				"Ruta del arhcivo donde se guardaran los objetos (dashboard y visualizaciones) generados");
		return options;
	}
}