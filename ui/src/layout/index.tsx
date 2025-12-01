import { memo, useEffect } from 'react';
import { Outlet } from 'react-router-dom';
import { ConfigProvider, message } from 'antd';
import { ConstantProvider } from '@/hooks';
import * as constants from "@/utils/constants";
import { setMessage } from '@/utils';
import UserAvatar from '@/components/UserAvatar';

// Layout 组件：应用的主要布局结构
const Layout: GenieType.FC = memo(() => {
  const [messageApi, messageContent] = message.useMessage();

  useEffect(() => {
    // 初始化全局 message
    setMessage(messageApi);
  }, [messageApi]);

  return (
    <ConfigProvider theme={{ token: { colorPrimary: '#4040FFB2' } }}>
      {messageContent}
      {/* 右上角用户头像 */}
      <div style={{ position: 'fixed', top: 16, right: 16, zIndex: 1000 }}>
        <UserAvatar />
      </div>
      {/* 暂时只有静态的 */}
      <ConstantProvider value={constants}>
        <Outlet />
      </ConstantProvider>
    </ConfigProvider>
  );
});

Layout.displayName = 'Layout';

export default Layout;
