package fr.cnrs.liris.SimAttack.Util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesManager {

	private static Properties defaultProps = null;
    private static final String PROPERTIES_FILENAME = PropertiesManager.class.getSimpleName()+".properties";


	public static String getProperty(String name) {
		return getProperties().getProperty(name);
	}

	private static Properties getProperties() {
        Properties properties = defaultProps;
        if (properties == null) {
            synchronized (PropertiesManager.class) {
                properties = defaultProps;
                if (properties == null) {
                    properties = defaultProps = getPropertiesFromClasspath();
                }
            }
        }
        return properties;
	}
	
	private static Properties getPropertiesFromClasspath() {
		Properties props = new Properties();
		try {
            InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("./fr/cnrs/liris/SimAttack/PropertiesManager.properties");
			props.load(stream);
			stream.close();
		} catch (IOException e) {	
			System.err.println(e);
			e.printStackTrace();
		}
		return props;
	}

	public static void test ()
	{
		System.out.println(Thread.currentThread().getContextClassLoader().getResource("./fr/cnrs/liris/SimAttack/PropertiesManager.properties"));
	}


}
