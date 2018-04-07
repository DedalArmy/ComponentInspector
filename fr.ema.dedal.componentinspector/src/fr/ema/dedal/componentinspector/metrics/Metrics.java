package fr.ema.dedal.componentinspector.metrics;

public class Metrics {
	
	private static int nbClasses = 0;
	private static int nbSpringXML = 0;
	private static int nbSpecs = 0;
	private static int nbConfs = 0;
	private static int nbAssembs = 0;
	private static int nbCompsClasses = 0;
	private static int nbCompsInst = 0;
	private static int nbCompsRoles = 0;
	private static int nbEmptySpringXML = 0;
	private static int nbSpecsEqualsConf = 0;
	private static int nbInterfaces = 0;
	private static int nbConnexions = 0;
	private static int nbConnexionlessArchis = 0;
	private static int nbCompClassMultiRoles= 0;
	private static int nbSplitInterfaces = 0;
	private static int nbAbstractedInterfaceType= 0;
	private static int nbFailedClass= 0;
	
	private Metrics() {}
	
	public static int getNbClasses() {
		return nbClasses;
	}
	public static void addNbClasses(){
		Metrics.nbClasses++;
	}
	public static int getNbSpringXML() {
		return nbSpringXML;
	}
	public static void addNbSpringXML() {
		Metrics.nbSpringXML++;
	}
	public static int getNbSpecs() {
		return nbSpecs;
	}
	public static void addNbSpecs() {
		Metrics.nbSpecs++;
	}
	public static int getNbSplitInterfaces() {
		return nbSplitInterfaces;
	}
	public static void addNbSplitInterfaces() {
		Metrics.nbSplitInterfaces++;
	}
	public static int getNbConfs() {
		return nbConfs;
	}
	public static void addNbConfs() {
		Metrics.nbConfs++;
	}
	public static int getNbAssembs() {
		return nbAssembs;
	}
	public static void addNbAssembs() {
		Metrics.nbAssembs++;
	}
	public static int getNbCompsClasses() {
		return nbCompsClasses;
	}
	public static void addNbCompsClasses() {
		Metrics.nbCompsClasses++;
	}
	public static int getNbCompsInst() {
		return nbCompsInst;
	}
	public static void addNbCompsInst() {
		Metrics.nbCompsInst++;
	}
	public static int getNbCompsRoles() {
		return nbCompsRoles;
	}
	public static void addNbCompsRoles() {
		Metrics.nbCompsRoles++;
	}
	public static int getNbEmptySpringXML() {
		return nbEmptySpringXML;
	}
	public static void addNbEmptySpringXML() {
		Metrics.nbEmptySpringXML++;
	}
	public static int getNbSpecsEqualsConf() {
		return nbSpecsEqualsConf;
	}
	public static void addNbSpecsEqualsConf() {
		Metrics.nbSpecsEqualsConf++;
	}
	public static int getNbInterfaces() {
		return nbInterfaces;
	}
	public static void addNbInterfaces() {
		Metrics.nbInterfaces++;
	}
	public static int getNbConnexions() {
		return nbConnexions;
	}
	public static void addNbConnexions() {
		Metrics.nbConnexions++;
	}
	public static int getNbConnexionlessArchis() {
		return nbConnexionlessArchis;
	}
	public static void addNbConnexionlessArchis() {
		Metrics.nbConnexionlessArchis++;
	}
	public static int getNbCompClassMultiRoles() {
		return nbCompClassMultiRoles;
	}
	public static void addNbCompClassMultiRoles() {
		Metrics.nbCompClassMultiRoles++;
	}
	public static int getNbAbstractedInterfaceType() {
		return nbAbstractedInterfaceType;
	}
	public static void addNbAbstractType() {
		Metrics.nbAbstractedInterfaceType++;
	}
	public static int getNbFailedClass() {
		return nbFailedClass;
	}
	public static void addNbFailedClass() {
		Metrics.nbFailedClass++;
	}
	
}
