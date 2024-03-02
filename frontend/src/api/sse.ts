import { useUserStore } from '@/stores/user'
import { storeToRefs } from 'pinia'
import { EventSourcePolyfill, NativeEventSource } from 'event-source-polyfill'
const EventSource = EventSourcePolyfill || NativeEventSource

function SSEConnect(roomUserNo: number) {
    const userStore = useUserStore()
    const { accessToken } = storeToRefs(userStore)
    const eventSource: EventSource = new EventSource(
        import.meta.env.VITE_API_BASE_URL + `/subscribe/${roomUserNo}`,
        {
            headers: {
                accessToken: 'bearer ' + accessToken.value,
                'Content-Type': 'text/event-stream'
            },
            heartbeatTimeout: 36000000
        }
    )
    return eventSource
}

export { SSEConnect }
