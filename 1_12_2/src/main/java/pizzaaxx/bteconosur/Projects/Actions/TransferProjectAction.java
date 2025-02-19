package pizzaaxx.bteconosur.Projects.Actions;

import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Projects.Project;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

public class TransferProjectAction {

    private final BTEConoSur plugin;
    private final Project project;
    private final UUID owner;

    public TransferProjectAction(BTEConoSur plugin, Project project, UUID owner) {
        this.plugin = plugin;
        this.project = project;
        this.owner = owner;
    }

    public void execute() throws SQLException, IOException {
        UUID oldOwner = project.getOwner();
        new AddMemberProjectAction(plugin, project, project.getOwner()).execute();
        new RemoveMemberProjectAction(plugin, project, owner).execute();
        new SetOwnerProjectAction(plugin, project, owner).execute();

        project.getCountry().getLogsChannel().sendMessage(":incoming_envelope: **" + plugin.getPlayerRegistry().get(oldOwner).getName() + "** ha transferido el proyecto `" + project.getId() + "` a **" + plugin.getPlayerRegistry().get(owner).getName() + "**.").queue();
    }
}
