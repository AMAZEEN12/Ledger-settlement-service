

import http from 'k6/http';

export const options = {
    scenarios:{

    steady_load: {
        executor: 'constant-arrival-rate',
        rate: 1200,
        timeUnit: '1s',
        duration: '10m',
        preAllocatedVUs: 200,
    maxVUs: 400,
    }
},
    thresholds: {
        http_req_duration: ['p(99)<1000'],
    },

};
export default function () {

    http.get('http://localhost:8080/payments/settlement?merchantId=MR-4471');
}