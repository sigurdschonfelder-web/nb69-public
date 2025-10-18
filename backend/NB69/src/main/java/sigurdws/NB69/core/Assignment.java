package sigurdws.NB69.core;

public class Assignment {

    private String task;
    private String name;

    public Assignment(String task, String name) {
        this.task = task;
        this.name = name;
    }

    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public String getPerson() {
        return name;
    }

    public void setPerson(String name) {
        this.name = name;
    }

}
