#!/usr/bin/env python3
"""
Cursor Agent 自动化脚本
- 启动 agent
- 每隔 5 秒输入 "你好"
- 1 分钟后尝试切换到 Claude 4.5 Opus
- 切换后发送 "你好" 测试
- 如果有未付款错误，切换回 Auto 继续刷 1 分钟
- 重复直到成功或手动退出
"""

import pexpect
import time
import sys
import io

# 配置
HELLO_INTERVAL = 5  # 每隔 5 秒说一次你好
INITIAL_DURATION = 1 * 60  # 初始阶段 1 分钟
RETRY_DURATION = 1 * 60  # 重试阶段 1 分钟
UNPAID_ERROR = "unpaid invoice"
TARGET_MODEL = "/model Claude 4.5 Opus"
FALLBACK_MODEL = "/model Auto"


class OutputCapture:
    """同时显示输出到终端并记录到缓冲区"""
    def __init__(self):
        self.buffer = ""
        self.recording = False
    
    def write(self, data):
        sys.stdout.write(data)
        sys.stdout.flush()
        if self.recording:
            self.buffer += data
    
    def flush(self):
        sys.stdout.flush()
    
    def start_recording(self):
        """开始记录"""
        self.buffer = ""
        self.recording = True
    
    def stop_recording(self):
        """停止记录并返回内容"""
        self.recording = False
        return self.buffer
    
    def get_buffer(self):
        """获取当前缓冲区内容"""
        return self.buffer
    
    def clear_buffer(self):
        """清空缓冲区"""
        self.buffer = ""


def log(msg):
    """打印带时间戳的日志"""
    timestamp = time.strftime("%H:%M:%S")
    print(f"\n[{timestamp}] {msg}")


def send_message(child, message):
    """发送消息并按回车"""
    child.send(message)
    time.sleep(0.1)
    child.send('\r')
    time.sleep(0.1)
    child.send('\r')


def send_hello_loop(child, duration):
    """在指定时间内每隔 HELLO_INTERVAL 秒发送 '你好'"""
    start_time = time.time()
    count = 0
    
    while time.time() - start_time < duration:
        count += 1
        log(f"发送: 你好 (第 {count} 次)")
        send_message(child, "你好")
        time.sleep(HELLO_INTERVAL)
    
    return count


def main():
    log("启动 Cursor Agent...")
    
    # 创建输出捕获器
    output_capture = OutputCapture()
    
    try:
        # 启动 agent
        child = pexpect.spawn('agent', encoding='utf-8', timeout=60)
        child.logfile = output_capture  # 使用自定义捕获器
        
        # 等待启动
        log("等待 Agent 启动...")
        
        # 循环等待直到看到输入框提示
        start_wait = time.time()
        max_wait = 60  # 最多等待 60 秒
        ready = False
        
        while time.time() - start_wait < max_wait:
            try:
                # 检测各种可能的状态
                index = child.expect([
                    'Plan, search, build anything',  # 启动成功的标志
                    'Trust this workspace',  # Workspace Trust 提示
                    'Workspace Trust',
                    pexpect.TIMEOUT
                ], timeout=3)
                
                if index == 0:
                    # 看到输入框提示，启动成功
                    log("检测到输入框，Agent 启动成功！")
                    ready = True
                    break
                elif index in [1, 2]:
                    # Workspace Trust 提示
                    log("检测到 Workspace Trust 提示，自动信任...")
                    child.send('a')
                    child.send('\r')
                    time.sleep(2)
                else:
                    # 超时，继续等待
                    pass
            except:
                time.sleep(1)
        
        if not ready:
            log("警告：未检测到启动成功标志，但继续执行...")
        
        log("Agent 已启动")
        time.sleep(1)
        
        attempt = 0
        
        while True:
            attempt += 1
            log(f"===== 第 {attempt} 轮 =====")
            
            # 刷消息阶段
            if attempt == 1:
                duration = INITIAL_DURATION
                log(f"开始发送消息，持续 {duration // 60} 分钟...")
            else:
                duration = RETRY_DURATION
                log(f"重试阶段，持续 {duration // 60} 分钟...")
            
            send_hello_loop(child, duration)
            
            # 尝试切换到 Claude 4.5 Opus
            log(f"尝试切换模型: {TARGET_MODEL}")
            send_message(child, TARGET_MODEL)
            time.sleep(3)
            
            # 开始记录输出
            output_capture.start_recording()
            
            # 切换后发送 "你好" 测试模型
            log("切换后测试: 发送 你好")
            send_message(child, "你好")
            
            # 等待响应
            time.sleep(20)
            
            # 尝试读取更多输出
            try:
                child.expect(pexpect.TIMEOUT, timeout=5)
            except:
                pass
            
            # 停止记录并获取输出
            output = output_capture.stop_recording()
            
            log("检查输出...")
            log(f"收到输出长度: {len(output)} 字符")
            
            # 检查是否有未付款错误
            if UNPAID_ERROR.lower() in output.lower():
                log("检测到未付款错误！")
                log(f"切换回: {FALLBACK_MODEL}")
                send_message(child, FALLBACK_MODEL)
                time.sleep(3)
                log("继续下一轮...")
                continue
            
            # 检查是否有正常回复
            # 去掉我们自己的日志信息，只检查实际输出
            clean_output = output.strip()
            
            # 排除只包含命令回显的情况
            has_valid_response = (
                len(clean_output) > 10 and  # 有一定长度
                "error" not in clean_output.lower() and  # 不包含 error
                "failed" not in clean_output.lower() and  # 不包含 failed
                ("你好" in clean_output or "帮" in clean_output or "？" in clean_output or "吗" in clean_output)  # 包含中文回复特征
            )
            
            if has_valid_response:
                log("检测到正常回复！模型切换成功！")
                log("正常退出程序。")
                send_message(child, "/exit")
                time.sleep(2)
                break
            else:
                log("未检测到有效回复，继续循环...")
                log(f"切换回: {FALLBACK_MODEL}")
                send_message(child, FALLBACK_MODEL)
                time.sleep(3)
                continue
                
    except pexpect.EOF:
        log("Agent 已退出。")
    except KeyboardInterrupt:
        log("\n用户中断，退出程序...")
    except Exception as e:
        log(f"发生错误: {e}")
        import traceback
        traceback.print_exc()
    finally:
        try:
            child.close()
        except:
            pass
        log("程序结束")


if __name__ == "__main__":
    main()
