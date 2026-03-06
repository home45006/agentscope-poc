package io.agentscope.poc.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import io.agentscope.poc.hook.CommandRegistry;
import io.agentscope.poc.model.MusicCommand;

import java.util.HashMap;
import java.util.Map;

public class MusicTools {

    @Tool(name = "play_music", description = "播放音乐：歌曲名、歌手、风格、心情等")
    public MusicCommand playMusic(
            @ToolParam(name = "query", description = "搜索关键词：歌曲名/歌手/风格/心情") String query,
            @ToolParam(name = "query_type", description = "查询类型：song/artist/genre/mood") String queryType) {
        Map<String, Object> params = new HashMap<>();
        params.put("query", query);
        params.put("query_type", queryType);

        MusicCommand cmd = new MusicCommand();
        cmd.action = "play_music";
        cmd.params = params;
        cmd.tts = "好的小安为您播放" + query;

        registerCommand("music", cmd);
        return cmd;
    }

    @Tool(name = "control_playback", description = "控制播放：暂停、继续、上一首、下一首")
    public MusicCommand controlPlayback(
            @ToolParam(name = "action", description = "操作：pause/resume/next/previous") String action) {
        Map<String, Object> params = new HashMap<>();
        params.put("action", action);

        MusicCommand cmd = new MusicCommand();
        cmd.action = "control_playback";
        cmd.params = params;
        cmd.tts = buildPlaybackTts(action);

        registerCommand("music", cmd);
        return cmd;
    }

    @Tool(name = "adjust_volume", description = "调节音量：增大、减小、设置为指定值")
    public MusicCommand adjustVolume(
            @ToolParam(name = "action", description = "操作：up/down/set") String action,
            @ToolParam(name = "value", description = "音量值（0-100），仅 set 时有效") Integer value) {
        Map<String, Object> params = new HashMap<>();
        params.put("action", action);
        if (value != null) params.put("value", value);

        MusicCommand cmd = new MusicCommand();
        cmd.action = "adjust_volume";
        cmd.params = params;
        cmd.tts = buildVolumeTts(action, value);

        registerCommand("music", cmd);
        return cmd;
    }

    @Tool(name = "set_play_mode", description = "切换播放模式：随机、单曲循环、顺序")
    public MusicCommand setPlayMode(
            @ToolParam(name = "mode", description = "模式：shuffle/repeat_one/sequence") String mode) {
        Map<String, Object> params = new HashMap<>();
        params.put("mode", mode);

        MusicCommand cmd = new MusicCommand();
        cmd.action = "set_play_mode";
        cmd.params = params;
        cmd.tts = buildModeTts(mode);

        registerCommand("music", cmd);
        return cmd;
    }

    private void registerCommand(String domain, MusicCommand cmd) {
        Map<String, Object> command = new HashMap<>();
        command.put("domain", domain);
        command.put("action", cmd.action);
        command.put("params", cmd.params);
        command.put("tts", cmd.tts);
        CommandRegistry.register(command);
    }

    private String buildPlaybackTts(String action) {
        return switch (action) {
            case "pause" -> "好的已暂停播放";
            case "resume" -> "好的已继续播放";
            case "next" -> "好的播放下一首";
            case "previous" -> "好的播放上一首";
            default -> "好的已完成操作";
        };
    }

    private String buildVolumeTts(String action, Integer value) {
        return switch (action) {
            case "up" -> "好的已调高音量";
            case "down" -> "好的已调低音量";
            case "set" -> "好的已将音量调至" + value;
            default -> "好的已完成操作";
        };
    }

    private String buildModeTts(String mode) {
        return switch (mode) {
            case "shuffle" -> "好的已切换随机播放";
            case "repeat_one" -> "好的已切换单曲循环";
            case "sequence" -> "好的已切换顺序播放";
            default -> "好的已完成操作";
        };
    }
}
