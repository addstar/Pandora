package au.com.addstar.pandora;

public interface Module
{
	void onEnable();
	
	void onDisable();
	
	void setPandoraInstance(MasterPlugin plugin);
}
