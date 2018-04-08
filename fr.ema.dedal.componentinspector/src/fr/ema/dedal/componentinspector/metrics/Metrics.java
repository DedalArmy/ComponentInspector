package fr.ema.dedal.componentinspector.metrics;

public class Metrics {
	
	private static double nbClasses = 0;
	private static double nbSpringXML = 0;
	private static double nbSpecs = 0;
	private static double nbConfs = 0;
	private static double nbAssembs = 0;
	private static double nbCompsClasses = 0;
	private static double nbCompsInst = 0;
	private static double nbCompsRoles = 0;
	private static double nbEmptySpringXML = 0;
	private static double nbSpecsEqualsConf = 0;
	private static double nbInterfaces = 0;
	private static double nbConnexions = 0;
	private static double nbConnexionlessArchis = 0;
	private static double nbCompClassMultiRoles= 0;
	private static double nbSplitInterfaces = 0;
	private static double nbAbstractedInterfaceType= 0;
	private static double nbFailedClass= 0;
	
	private Metrics() {}
	
	public static double getNbClasses() {
		return nbClasses;
	}
	public static void addNbClasses(){
		Metrics.nbClasses++;
	}
	public static double getNbSpringXML() {
		return nbSpringXML;
	}
	public static void addNbSpringXML() {
		Metrics.nbSpringXML++;
	}
	public static double getNbSpecs() {
		return nbSpecs;
	}
	public static void addNbSpecs() {
		Metrics.nbSpecs++;
	}
	public static double getNbSplitInterfaces() {
		return nbSplitInterfaces;
	}
	public static void addNbSplitInterfaces() {
		Metrics.nbSplitInterfaces++;
	}
	public static double getNbConfs() {
		return nbConfs;
	}
	public static void addNbConfs() {
		Metrics.nbConfs++;
	}
	public static double getNbAssembs() {
		return nbAssembs;
	}
	public static void addNbAssembs() {
		Metrics.nbAssembs++;
	}
	public static double getNbCompsClasses() {
		return nbCompsClasses;
	}
	public static void addNbCompsClasses() {
		Metrics.nbCompsClasses++;
	}
	public static double getNbCompsInst() {
		return nbCompsInst;
	}
	public static void addNbCompsInst() {
		Metrics.nbCompsInst++;
	}
	public static double getNbCompsRoles() {
		return nbCompsRoles;
	}
	public static void addNbCompsRoles() {
		Metrics.nbCompsRoles++;
	}
	public static double getNbEmptySpringXML() {
		return nbEmptySpringXML;
	}
	public static void addNbEmptySpringXML() {
		Metrics.nbEmptySpringXML++;
	}
	public static double getNbSpecsEqualsConf() {
		return nbSpecsEqualsConf;
	}
	public static void addNbSpecsEqualsConf() {
		Metrics.nbSpecsEqualsConf++;
	}
	public static double getNbInterfaces() {
		return nbInterfaces;
	}
	public static void addNbInterfaces() {
		Metrics.nbInterfaces++;
	}
	public static double getNbConnexions() {
		return nbConnexions;
	}
	public static void addNbConnexions() {
		Metrics.nbConnexions++;
	}
	public static double getNbConnexionlessArchis() {
		return nbConnexionlessArchis;
	}
	public static void addNbConnexionlessArchis() {
		Metrics.nbConnexionlessArchis++;
	}
	public static double getNbCompClassMultiRoles() {
		return nbCompClassMultiRoles;
	}
	public static void addNbCompClassMultiRoles() {
		Metrics.nbCompClassMultiRoles++;
	}
	public static double getNbAbstractedInterfaceType() {
		return nbAbstractedInterfaceType;
	}
	public static void addNbAbstractType() {
		Metrics.nbAbstractedInterfaceType++;
	}
	public static double getNbFailedClass() {
		return nbFailedClass;
	}
	public static void addNbFailedClass() {
		Metrics.nbFailedClass++;
	}
	
}
