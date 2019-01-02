package config;

import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

/**
 * 全局配置
 *
 * Created by Yuk on 2019/1/1.
 */
public class NettyConfig {
    /**
     * 存储每一个客户端接入进来时的channel对象
     */

    public static ChannelGroup group = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
}
