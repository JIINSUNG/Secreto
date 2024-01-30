import type { AxiosInstance, AxiosResponse } from 'axios'
import { localAxios } from '@/utils/http-commons'
const roomInstance: AxiosInstance = localAxios()

async function createRoom(
    param: object,
    success: (response: AxiosResponse) => void,
    fail: (error: any) => void
) {
    roomInstance.post(`/room`, JSON.stringify(param)).then(success).catch(fail)
}

async function getRoom(
    param: number,
    success: (response: AxiosResponse) => void,
    fail: (error: any) => void
) {
    roomInstance.get(`/room/${param}`).then(success).catch(fail)
}

async function changeRoomName(
    param: object,
    success: (response: AxiosResponse) => void,
    fail: (error: any) => void
) {
    roomInstance.put(`/room/`, JSON.stringify(param)).then(success).catch(fail)
}

export { createRoom, getRoom, changeRoomName }