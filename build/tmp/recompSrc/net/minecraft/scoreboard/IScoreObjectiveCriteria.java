package net.minecraft.scoreboard;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface IScoreObjectiveCriteria
{
    Map INSTANCES = new HashMap();
    IScoreObjectiveCriteria DUMMY = new ScoreDummyCriteria("dummy");
    IScoreObjectiveCriteria deathCount = new ScoreDummyCriteria("deathCount");
    IScoreObjectiveCriteria playerKillCount = new ScoreDummyCriteria("playerKillCount");
    IScoreObjectiveCriteria totalKillCount = new ScoreDummyCriteria("totalKillCount");
    IScoreObjectiveCriteria health = new ScoreHealthCriteria("health");

    String getName();

    int func_96635_a(List p_96635_1_);

    boolean isReadOnly();
}